package com.erp.server.sales.order.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.inventory.service.InventoryService;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.master.customer.repository.CustomerRepository;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.repository.ItemRepository;
import com.erp.server.sales.order.domain.SalesOrder;
import com.erp.server.sales.order.domain.SalesOrderItem;
import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.order.dto.SalesOrderCancelRequest;
import com.erp.server.sales.order.dto.SalesOrderCancelResponse;
import com.erp.server.sales.order.dto.SalesOrderCreateRequest;
import com.erp.server.sales.order.dto.SalesOrderDetailResponse;
import com.erp.server.sales.order.dto.SalesOrderItemRequest;
import com.erp.server.sales.order.dto.SalesOrderItemResponse;
import com.erp.server.sales.order.dto.SalesOrderListResponse;
import com.erp.server.sales.order.dto.SalesOrderRegisterResponse;
import com.erp.server.sales.order.dto.SalesOrderShipmentResponse;
import com.erp.server.sales.order.dto.SalesOrderUpdateRequest;
import com.erp.server.sales.order.repository.SalesOrderItemRepository;
import com.erp.server.sales.order.repository.SalesOrderRepository;
import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.DeliveryNoteStatus;
import com.erp.server.sales.shipment.domain.Shipment;
import com.erp.server.sales.shipment.domain.ShipmentLot;
import com.erp.server.sales.shipment.domain.ShipmentStatus;
import com.erp.server.sales.shipment.repository.DeliveryNoteRepository;
import com.erp.server.sales.shipment.repository.ShipmentLotRepository;
import com.erp.server.sales.shipment.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

// ********** 주문 목록·상세·작성과 접수·취소 업무 규칙 및 연결 출고 처리를 담당하는 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesOrderService {

	private static final int SALES_ORDER_PAGE_SIZE = 20;

	private final AppUserRepository appUserRepository;
	private final CustomerRepository customerRepository;
	private final ItemRepository itemRepository;
	private final SalesOrderRepository salesOrderRepository;
	private final SalesOrderItemRepository salesOrderItemRepository;
	private final ShipmentRepository shipmentRepository;
	private final ShipmentLotRepository shipmentLotRepository;
	private final DeliveryNoteRepository deliveryNoteRepository;
	private final InventoryService inventoryService;

	// ========== 거래처·상태·등록 기간 조건을 적용하여 주문 목록을 페이지 조회하는 메서드 ==========
	// 기간은 createdAt 기준 양끝 날짜를 포함하고 페이지당 20건, 등록 일시·주문 식별자 내림차순으로 고정한다.
	public Page<SalesOrderListResponse> getSalesOrders(Long customerId, SalesOrderStatus status,
			LocalDate startDate, LocalDate endDate, int page, UserRole currentUserRole) {
		validatePage(page);
		validateDateRange(startDate, endDate);
		LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
		PageRequest pageable = PageRequest.of(page, SALES_ORDER_PAGE_SIZE,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("salesOrderId")));

		return salesOrderRepository.findAllByFilters(customerId, status, startDateTime, endDateTime, pageable)
				.map(order -> createListResponse(order, currentUserRole));
	}

	// ========== salesOrderId로 주문 기본정보·품목·처리 이력·연결 출고를 상세 조회하는 메서드 ==========
	public SalesOrderDetailResponse getSalesOrder(Long salesOrderId, UserRole currentUserRole) {
		SalesOrder salesOrder = findSalesOrderDetail(salesOrderId);
		List<SalesOrderItem> items = salesOrderItemRepository.findAllBySalesOrderIdWithItem(salesOrderId);
		Shipment shipment = shipmentRepository.findBySalesOrderId(salesOrderId).orElse(null);
		return createDetailResponse(salesOrder, items, shipment, currentUserRole);
	}

	// ========== ACTIVE 거래처·품목과 거래처 기본 배송정보를 적용하여 DRAFT 주문을 생성하는 메서드 ==========
	// 판매 단가를 입력하지 않은 품목은 현재 ITEM.defaultSalesPrice를 적용하며 주문 접수 전까지 변경할 수 있다.
	@Transactional
	public SalesOrderDetailResponse createSalesOrder(SalesOrderCreateRequest request, Long currentUserId) {
		validateDuplicateItems(request.items());
		AppUser currentUser = findUser(currentUserId);
		Customer customer = findCustomerForUpdate(request.customerId());
		validateActiveCustomer(customer);
		List<Item> items = findAndValidateItems(request.items());

		SalesOrder salesOrder = SalesOrder.create(customer, request.channel(),
				firstNonBlank(request.deliveryPostalCode(), customer.getDeliveryPostalCode()),
				firstNonBlank(request.deliveryAddress(), customer.getDeliveryAddress()),
				firstNonBlank(request.deliveryAddressDetail(), customer.getDeliveryAddressDetail()),
				firstNonBlank(request.recipientName(), customer.getRecipientName()),
				firstNonBlank(request.recipientPhone(), customer.getRecipientPhone()),
				normalizeOptionalValue(request.memo()), currentUser);
		addSalesOrderItems(salesOrder, request.items(), items);
		salesOrderRepository.save(salesOrder);
		flushSalesOrderChanges();

		return createDetailResponse(salesOrder, salesOrder.getItems(), null, currentUser.getRole());
	}

	// ========== DRAFT 상태와 version을 검증하고 주문 기본정보·배송정보·품목을 수정하는 메서드 ==========
	@Transactional
	public SalesOrderDetailResponse updateSalesOrder(Long salesOrderId, SalesOrderUpdateRequest request,
			Long currentUserId) {
		validateDuplicateItems(request.items());
		SalesOrder salesOrder = findSalesOrderForUpdate(salesOrderId);
		validateVersion(salesOrder, request.version());
		validateStatus(salesOrder, SalesOrderStatus.DRAFT, "작성 중인 주문만 수정할 수 있습니다.");
		AppUser currentUser = findUser(currentUserId);
		Customer customer = findCustomerForUpdate(request.customerId());
		validateActiveCustomer(customer);
		List<Item> items = findAndValidateItems(request.items());

		// 기존 자식 행을 먼저 삭제하여 같은 품목과 표시 순번을 다시 사용할 때 UNIQUE 제약조건이 충돌하지 않게 한다.
		salesOrderItemRepository.deleteAllBySalesOrderId(salesOrderId);
		salesOrder.clearItems();
		salesOrder.updateDraft(customer, request.channel(),
				firstNonBlank(request.deliveryPostalCode(), customer.getDeliveryPostalCode()),
				firstNonBlank(request.deliveryAddress(), customer.getDeliveryAddress()),
				firstNonBlank(request.deliveryAddressDetail(), customer.getDeliveryAddressDetail()),
				firstNonBlank(request.recipientName(), customer.getRecipientName()),
				firstNonBlank(request.recipientPhone(), customer.getRecipientPhone()),
				normalizeOptionalValue(request.memo()));
		addSalesOrderItems(salesOrder, request.items(), items);
		flushSalesOrderChanges();

		return createDetailResponse(salesOrder, salesOrder.getItems(), null, currentUser.getRole());
	}

	// ========== DRAFT 상태와 version을 검증하고 주문과 주문 품목을 물리 삭제하는 메서드 ==========
	@Transactional
	public void deleteSalesOrder(Long salesOrderId, Long requestVersion) {
		SalesOrder salesOrder = findSalesOrderForUpdate(salesOrderId);
		validateVersion(salesOrder, requestVersion);
		validateStatus(salesOrder, SalesOrderStatus.DRAFT, "작성 중인 주문만 삭제할 수 있습니다.");
		salesOrderRepository.delete(salesOrder);
		flushSalesOrderChanges();
	}

	// ========== 최신 거래처·품목·필수 배송정보를 검증하고 주문 접수와 PENDING 출고 생성을 한 트랜잭션으로 처리하는 메서드 ==========
	// 주문 접수 시 창고·LOT를 선택하거나 재고를 예약하지 않으므로 재고 부족은 접수를 차단하지 않는다.
	@Transactional
	public SalesOrderRegisterResponse registerSalesOrder(Long salesOrderId, Long requestVersion, Long currentUserId) {
		SalesOrder salesOrder = findSalesOrderForUpdate(salesOrderId);
		validateVersion(salesOrder, requestVersion);
		validateStatus(salesOrder, SalesOrderStatus.DRAFT, "작성 중인 주문만 접수할 수 있습니다.");
		AppUser currentUser = findUser(currentUserId);

		Customer latestCustomer = findCustomerForUpdate(salesOrder.getCustomer().getCustomerId());
		validateActiveCustomer(latestCustomer);
		if (latestCustomer.getTradeStatus() == CustomerTradeStatus.HOLD) {
			throw new BusinessException(ErrorCode.CONFLICT, "거래 중지 상태의 거래처 주문은 접수할 수 없습니다.");
		}

		List<SalesOrderItem> orderItems = salesOrderItemRepository.findAllBySalesOrderIdWithItem(salesOrderId);
		validateStoredItems(orderItems);
		validateRequiredDeliveryInformation(salesOrder);
		salesOrder.register(currentUser);
		Shipment shipment = shipmentRepository.save(Shipment.createPending(salesOrder));
		flushSalesOrderChanges();
		shipmentRepository.flush();

		return new SalesOrderRegisterResponse(salesOrder.getSalesOrderId(), salesOrder.getStatus(),
				salesOrder.getVersion(), shipment.getShipmentId(), shipment.getStatus(), shipment.getVersion());
	}

	// ========== REGISTERED 주문의 연결 출고 상태에 따라 예약·납품서를 정리하고 주문과 출고를 함께 취소하는 메서드 ==========
	// PENDING은 즉시 취소하고 PACKED는 LOT 예약 해제와 ACTIVE 납품서 무효 처리 후 취소하며 COMPLETED는 거부한다.
	@Transactional
	public SalesOrderCancelResponse cancelSalesOrder(Long salesOrderId, SalesOrderCancelRequest request,
			Long currentUserId) {
		SalesOrder salesOrder = findSalesOrderForUpdate(salesOrderId);
		validateVersion(salesOrder, request.version());
		validateStatus(salesOrder, SalesOrderStatus.REGISTERED, "접수된 주문만 취소할 수 있습니다.");
		AppUser currentUser = findUser(currentUserId);
		Shipment shipment = shipmentRepository.findBySalesOrderIdForUpdate(salesOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "주문에 연결된 출고를 찾을 수 없습니다."));

		if (shipment.getStatus() == ShipmentStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.CONFLICT, "출고가 완료된 주문은 취소할 수 없습니다.");
		}
		if (shipment.getStatus() == ShipmentStatus.CANCELED) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 취소된 출고입니다.");
		}

		List<ShipmentLot> reservedLots = shipment.getStatus() == ShipmentStatus.PACKED
				? shipmentLotRepository.findReservedByShipmentIdForUpdate(shipment.getShipmentId()) : List.of();
		BigDecimal releasedQuantity = BigDecimal.ZERO;
		if (!reservedLots.isEmpty()) {
			List<Long> inventoryLotIds = reservedLots.stream()
					.map(shipmentLot -> shipmentLot.getInventoryLot().getInventoryLotId()).distinct().sorted().toList();
			inventoryService.getInventoryLotsForUpdate(inventoryLotIds);
			for (ShipmentLot shipmentLot : reservedLots) {
				inventoryService.releaseShipmentReservation(shipmentLot.getInventoryLot().getInventoryLotId(),
						shipmentLot.getPackedQuantity());
				shipmentLot.releaseReservation();
				releasedQuantity = releasedQuantity.add(shipmentLot.getPackedQuantity());
			}
		}

		if (shipment.getStatus() == ShipmentStatus.PACKED) {
			List<DeliveryNote> deliveryNotes = deliveryNoteRepository.findAllByShipmentIdAndStatusForUpdate(
					shipment.getShipmentId(), DeliveryNoteStatus.ACTIVE);
			deliveryNotes.forEach(note -> note.voidNote(currentUser, request.reason().trim()));
		}

		shipment.cancel(currentUser);
		salesOrder.cancel(currentUser, request.reason().trim());
		flushSalesOrderChanges();
		shipmentRepository.flush();

		return new SalesOrderCancelResponse(salesOrder.getSalesOrderId(), salesOrder.getStatus(),
				shipment.getShipmentId(), shipment.getStatus(), reservedLots.size(), releasedQuantity);
	}

	// ========== 요청 순서대로 DRAFT 주문 품목을 생성하고 주문 총액을 계산하는 메서드 ==========
	private void addSalesOrderItems(SalesOrder salesOrder, List<SalesOrderItemRequest> requests, List<Item> items) {
		Map<Long, Item> itemMap = new HashMap<>();
		items.forEach(item -> itemMap.put(item.getItemId(), item));
		for (int index = 0; index < requests.size(); index++) {
			SalesOrderItemRequest request = requests.get(index);
			Item item = itemMap.get(request.itemId());
			BigDecimal unitPrice = request.unitPrice() == null ? item.getDefaultSalesPrice() : request.unitPrice();
			salesOrder.addItem(SalesOrderItem.create(salesOrder, index + 1, item,
					request.orderQuantity(), unitPrice));
		}
	}

	// ========== 주문 목록 Entity를 역할별 금액 공개 범위에 맞는 응답으로 변환하는 메서드 ==========
	private SalesOrderListResponse createListResponse(SalesOrder order, UserRole role) {
		boolean warehouse = role == UserRole.WAREHOUSE;
		String customerCode = order.getStatus() == SalesOrderStatus.DRAFT
				? order.getCustomer().getCustomerCode() : order.getCustomerCodeSnapshot();
		String customerName = order.getStatus() == SalesOrderStatus.DRAFT
				? order.getCustomer().getCustomerName() : order.getCustomerNameSnapshot();
		return new SalesOrderListResponse(order.getSalesOrderId(), order.getCustomer().getCustomerId(),
				customerCode, customerName, order.getChannel(), order.getStatus(), order.getCustomer().getTradeStatus(),
				order.getCustomer().getTradeStatus() == CustomerTradeStatus.HOLD,
				warehouse ? null : order.getTotalAmount(), order.getCreatedAt(), order.getRegisteredAt(), order.getVersion());
	}

	// ========== 주문·품목·출고 Entity를 역할별 금액 공개 범위에 맞는 상세 응답으로 변환하는 메서드 ==========
	private SalesOrderDetailResponse createDetailResponse(SalesOrder order, List<SalesOrderItem> items,
			Shipment shipment, UserRole role) {
		boolean warehouse = role == UserRole.WAREHOUSE;
		List<SalesOrderItemResponse> itemResponses = items.stream().map(item -> new SalesOrderItemResponse(
				item.getSalesOrderItemId(), item.getLineNo(), item.getItem().getItemId(), item.getItemCodeSnapshot(),
				item.getItemNameSnapshot(), item.getUnitSnapshot(), item.getOrderQuantity(),
				warehouse ? null : item.getUnitPrice(), warehouse ? null : item.getLineAmount())).toList();
		SalesOrderShipmentResponse shipmentResponse = createShipmentResponse(shipment);

		return new SalesOrderDetailResponse(order.getSalesOrderId(), order.getCustomer().getCustomerId(),
				order.getCustomerCodeSnapshot(), order.getCustomerNameSnapshot(), order.getCustomer().getStatus(),
				order.getCustomer().getTradeStatus(), order.getCustomer().getTradeStatus() == CustomerTradeStatus.HOLD,
				order.getChannel(), order.getStatus(), order.getDeliveryPostalCodeSnapshot(),
				order.getDeliveryAddressSnapshot(), order.getDeliveryAddressDetailSnapshot(),
				order.getRecipientNameSnapshot(), order.getRecipientPhoneSnapshot(),
				warehouse ? null : order.getTotalAmount(), order.getMemo(), itemResponses, shipmentResponse,
				order.getCreatedBy().getUserId(), order.getCreatedBy().getUserName(), order.getCreatedAt(),
				order.getRegisteredBy() == null ? null : order.getRegisteredBy().getUserId(),
				order.getRegisteredBy() == null ? null : order.getRegisteredBy().getUserName(), order.getRegisteredAt(),
				order.getCanceledBy() == null ? null : order.getCanceledBy().getUserId(),
				order.getCanceledBy() == null ? null : order.getCanceledBy().getUserName(), order.getCanceledAt(),
				order.getCancelReason(), order.getUpdatedAt(), order.getVersion());
	}

	private SalesOrderShipmentResponse createShipmentResponse(Shipment shipment) {
		if (shipment == null) return null;
		return new SalesOrderShipmentResponse(shipment.getShipmentId(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseId(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseCode(),
				shipment.getWarehouse() == null ? null : shipment.getWarehouse().getWarehouseName(),
				shipment.getStatus(), shipment.getPackingSequence(), shipment.getVersion());
	}

	// ========== 요청 품목을 식별자 오름차순으로 잠그고 존재 여부와 ACTIVE 상태를 검증하는 메서드 ==========
	private List<Item> findAndValidateItems(List<SalesOrderItemRequest> requests) {
		List<Long> itemIds = requests.stream().map(SalesOrderItemRequest::itemId).distinct().sorted().toList();
		List<Item> items = itemRepository.findAllByIdsForUpdate(itemIds);
		if (items.size() != itemIds.size()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "주문 품목을 찾을 수 없습니다.");
		}
		items.forEach(this::validateActiveItem);
		return items;
	}

	// ========== 주문 접수 직전 저장 품목을 다시 잠금 조회하여 삭제·사용 중지된 품목이 없는지 검증하는 메서드 ==========
	private void validateStoredItems(List<SalesOrderItem> orderItems) {
		if (orderItems.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "주문 품목을 하나 이상 등록해 주세요.");
		}
		List<Long> itemIds = orderItems.stream().map(item -> item.getItem().getItemId()).distinct().sorted().toList();
		List<Item> latestItems = itemRepository.findAllByIdsForUpdate(itemIds);
		if (latestItems.size() != itemIds.size()) {
			throw new BusinessException(ErrorCode.CONFLICT, "삭제된 주문 품목이 있어 주문을 접수할 수 없습니다.");
		}
		latestItems.forEach(this::validateActiveItem);
	}

	private void validateActiveCustomer(Customer customer) {
		if (customer.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 거래처만 주문에 사용할 수 있습니다.");
		}
	}

	private void validateActiveItem(Item item) {
		if (item.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 품목만 주문에 사용할 수 있습니다.");
		}
	}

	// ========== 주문 접수에 필요한 배송지 주소·수령인·수령인 연락처가 모두 입력되었는지 검증하는 메서드 ==========
	private void validateRequiredDeliveryInformation(SalesOrder order) {
		if (isBlank(order.getDeliveryAddressSnapshot()) || isBlank(order.getRecipientNameSnapshot())
				|| isBlank(order.getRecipientPhoneSnapshot())) {
			throw new BusinessException(ErrorCode.CONFLICT, "주문 접수 전에 배송지 주소, 수령인과 연락처를 입력해 주세요.");
		}
	}

	private void validateDuplicateItems(List<SalesOrderItemRequest> items) {
		Set<Long> itemIds = new HashSet<>();
		for (SalesOrderItemRequest item : items) {
			if (!itemIds.add(item.itemId())) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 품목을 주문에 중복 등록할 수 없습니다.");
			}
		}
	}

	private void validateVersion(SalesOrder order, Long requestVersion) {
		if (requestVersion == null || !requestVersion.equals(order.getVersion())) {
			throw createVersionConflictException();
		}
	}

	private void validateStatus(SalesOrder order, SalesOrderStatus expected, String message) {
		if (order.getStatus() != expected) throw new BusinessException(ErrorCode.CONFLICT, message);
	}

	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "처리 사용자를 찾을 수 없습니다."));
	}

	private Customer findCustomerForUpdate(Long customerId) {
		return customerRepository.findByIdForUpdate(customerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "거래처를 찾을 수 없습니다."));
	}

	private SalesOrder findSalesOrderForUpdate(Long salesOrderId) {
		return salesOrderRepository.findByIdForUpdate(salesOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "주문을 찾을 수 없습니다."));
	}

	private SalesOrder findSalesOrderDetail(Long salesOrderId) {
		return salesOrderRepository.findDetailById(salesOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "주문을 찾을 수 없습니다."));
	}

	// ========== UPDATE를 즉시 실행하여 최종 낙관적 잠금 충돌을 현재 요청 안에서 확인하는 메서드 ==========
	private void flushSalesOrderChanges() {
		try {
			salesOrderRepository.flush();
		} catch (OptimisticLockingFailureException exception) {
			throw createVersionConflictException();
		}
	}

	private BusinessException createVersionConflictException() {
		return new BusinessException(ErrorCode.CONFLICT,
				"다른 사용자가 먼저 주문을 수정하거나 처리했습니다. 최신 주문 정보를 다시 조회해 주세요.");
	}

	private String firstNonBlank(String requested, String defaultValue) {
		String normalized = normalizeOptionalValue(requested);
		return normalized == null ? normalizeOptionalValue(defaultValue) : normalized;
	}

	private String normalizeOptionalValue(String value) {
		if (value == null) return null;
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private void validatePage(int page) {
		if (page < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 늦을 수 없습니다.");
		}
	}
}
