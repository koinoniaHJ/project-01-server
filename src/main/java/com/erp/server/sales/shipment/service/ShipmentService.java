package com.erp.server.sales.shipment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.inventory.domain.InventoryLotStatus;
import com.erp.server.inventory.repository.InventoryLotRepository;
import com.erp.server.inventory.service.InventoryService;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.customer.domain.Customer;
import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.master.customer.repository.CustomerRepository;
import com.erp.server.master.warehouse.domain.Warehouse;
import com.erp.server.master.warehouse.repository.WarehouseRepository;
import com.erp.server.sales.order.domain.SalesOrder;
import com.erp.server.sales.order.domain.SalesOrderItem;
import com.erp.server.sales.order.domain.SalesOrderStatus;
import com.erp.server.sales.order.repository.SalesOrderItemRepository;
import com.erp.server.sales.order.repository.SalesOrderRepository;
import com.erp.server.sales.shipment.domain.DeliveryNote;
import com.erp.server.sales.shipment.domain.DeliveryNoteStatus;
import com.erp.server.sales.shipment.domain.Shipment;
import com.erp.server.sales.shipment.domain.ShipmentLot;
import com.erp.server.sales.shipment.domain.ShipmentStatus;
import com.erp.server.sales.shipment.document.DeliveryNoteDocumentData;
import com.erp.server.sales.shipment.document.DeliveryNotePdfService;
import com.erp.server.sales.shipment.dto.ShipmentAvailableLotResponse;
import com.erp.server.sales.shipment.dto.ShipmentCompleteResponse;
import com.erp.server.sales.shipment.dto.ShipmentDetailResponse;
import com.erp.server.sales.shipment.dto.ShipmentListResponse;
import com.erp.server.sales.shipment.dto.ShipmentLotAllocationRequest;
import com.erp.server.sales.shipment.dto.ShipmentPackResponse;
import com.erp.server.sales.shipment.dto.ShipmentPackingRequest;
import com.erp.server.sales.shipment.dto.ShipmentUnpackRequest;
import com.erp.server.sales.shipment.dto.ShipmentUnpackResponse;
import com.erp.server.sales.shipment.repository.DeliveryNoteRepository;
import com.erp.server.sales.shipment.repository.ShipmentLotRepository;
import com.erp.server.sales.shipment.repository.ShipmentRepository;
import com.erp.server.settlement.domain.Voucher;
import com.erp.server.settlement.service.SettlementService;
import com.erp.server.settlement.service.VoucherItemInput;

import lombok.RequiredArgsConstructor;

// ********** 출고 목록·상세·포장안·예약·완료와 재고·납품서·매출 전표 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentService {

	private static final int SHIPMENT_PAGE_SIZE = 20;

	private final AppUserRepository appUserRepository;
	private final CustomerRepository customerRepository;
	private final WarehouseRepository warehouseRepository;
	private final SalesOrderRepository salesOrderRepository;
	private final SalesOrderItemRepository salesOrderItemRepository;
	private final ShipmentRepository shipmentRepository;
	private final ShipmentLotRepository shipmentLotRepository;
	private final DeliveryNoteRepository deliveryNoteRepository;
	private final InventoryLotRepository inventoryLotRepository;
	private final InventoryService inventoryService;
	private final SettlementService settlementService;
	private final DeliveryNotePdfService deliveryNotePdfService;

	// ========== 거래처·상태·주문 접수 기간 조건을 적용하여 출고 목록을 최신순으로 페이지 조회하는 메서드 ==========
	public Page<ShipmentListResponse> getShipments(Long customerId, ShipmentStatus status,
			LocalDate startDate, LocalDate endDate, int page, UserRole role) {
		validatePage(page);
		validateDateRange(startDate, endDate);
		LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
		PageRequest pageable = PageRequest.of(page, SHIPMENT_PAGE_SIZE,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("shipmentId")));
		return shipmentRepository.findAllByFilters(customerId, status, startDateTime, endDateTime, pageable)
				.map(shipment -> ShipmentListResponse.from(shipment, role));
	}

	// ========== shipmentId로 주문·배송정보·포장 LOT·납품서·처리 이력을 상세 조회하는 메서드 ==========
	public ShipmentDetailResponse getShipment(Long shipmentId, UserRole role) {
		Shipment shipment = findShipmentDetail(shipmentId);
		return createDetailResponse(shipment, role);
	}

	// ========== 요청 회차의 ACTIVE 납품서와 현재 포장 LOT를 조회하여 판매 금액 없는 PDF를 생성하는 메서드 ==========
	public byte[] getDeliveryNotePdf(Long shipmentId, Integer issueSequence) {
		if (issueSequence == null || issueSequence < 1) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "납품서 발행 회차는 1 이상이어야 합니다.");
		}
		DeliveryNote deliveryNote = deliveryNoteRepository.findByShipmentIdAndIssueSequenceAndStatus(
				shipmentId, issueSequence, DeliveryNoteStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
						"현재 유효한 납품서를 찾을 수 없습니다."));
		List<ShipmentLot> shipmentLots = shipmentLotRepository.findAllByShipmentIdWithDetails(shipmentId);
		if (shipmentLots.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "납품서에 출력할 출고 LOT를 찾을 수 없습니다.");
		}
		return deliveryNotePdfService.createDeliveryNotePdf(DeliveryNoteDocumentData.from(deliveryNote, shipmentLots));
	}

	// ========== 선택 창고에서 주문 품목별 출고 가능 LOT를 사용기한·입고 순서로 조회하는 메서드 ==========
	// 목록 단계에서 실사/조정 제한 LOT를 제외하고 포장 저장·확정 시 잠금 후 동일 조건을 다시 검증한다.
	public List<ShipmentAvailableLotResponse> getAvailableLots(Long shipmentId, Long warehouseId) {
		Shipment shipment = findShipmentDetail(shipmentId);
		validateShipmentStatus(shipment, ShipmentStatus.PENDING, "포장 대기 중인 출고만 LOT를 선택할 수 있습니다.");
		validateOrderRegistered(shipment.getSalesOrder());
		Warehouse warehouse = findActiveWarehouse(warehouseId);
		List<SalesOrderItem> orderItems = findOrderItems(shipment.getSalesOrder().getSalesOrderId());
		Map<Long, SalesOrderItem> orderItemByItemId = orderItems.stream()
				.collect(Collectors.toMap(item -> item.getItem().getItemId(), Function.identity()));
		List<InventoryLot> inventoryLots = inventoryLotRepository.findAvailableForShipment(warehouse.getWarehouseId(),
				orderItemByItemId.keySet(), InventoryLotStatus.AVAILABLE, LocalDate.now());

		return inventoryLots.stream().filter(lot -> !inventoryService.hasInventoryWorkRestriction(lot))
				.map(lot -> ShipmentAvailableLotResponse.from(
						orderItemByItemId.get(lot.getItem().getItemId()).getSalesOrderItemId(), lot))
				.toList();
	}

	// ========== PENDING 출고의 단일 창고·LOT별 포장안을 저장하되 재고 예약은 변경하지 않는 메서드 ==========
	// SHIPMENT_LOT은 현재 포장안이므로 다시 저장하면 기존 미예약 행을 삭제하고 요청 전체로 교체한다.
	@Transactional
	public ShipmentDetailResponse savePacking(Long shipmentId, ShipmentPackingRequest request, UserRole role) {
		LockedShipment locked = findOrderAndShipmentForUpdate(shipmentId);
		Shipment shipment = locked.shipment();
		validateVersion(shipment, request.version());
		validateShipmentStatus(shipment, ShipmentStatus.PENDING, "포장 대기 중인 출고만 포장안을 저장할 수 있습니다.");
		validateOrderRegistered(locked.order());
		Warehouse warehouse = findActiveWarehouseForUpdate(request.warehouseId());
		List<SalesOrderItem> orderItems = findOrderItems(locked.order().getSalesOrderId());
		ValidatedPacking packing = validatePackingRequest(request.lotAllocations(), orderItems, warehouse, false);
		List<ShipmentLot> existingLots = shipmentLotRepository.findAllByShipmentIdForUpdate(shipmentId);
		if (existingLots.stream().anyMatch(ShipmentLot::isReserved)) {
			throw new BusinessException(ErrorCode.CONFLICT, "예약이 반영된 출고는 포장 취소 후 포장안을 변경해 주세요.");
		}

		shipmentLotRepository.deleteAllByShipmentId(shipmentId);
		shipment.savePackingPlan(warehouse);
		List<ShipmentLot> savedLots = packing.inputs().stream().map(input -> shipmentLotRepository.save(
				ShipmentLot.create(shipment, input.orderItem(), input.inventoryLot(), input.quantity()))).toList();
		flushShipmentChanges();

		List<DeliveryNote> deliveryNotes = deliveryNoteRepository.findAllByShipmentIdWithUsers(shipmentId);
		return ShipmentDetailResponse.from(shipment, orderItems, savedLots, deliveryNotes, role,
				findRestrictedInventoryLotIds(savedLots));
	}

	// ========== 저장된 포장 수량을 최신 LOT 재고에 예약하고 PACKED 상태와 ACTIVE 납품서를 생성하는 메서드 ==========
	@Transactional
	public ShipmentPackResponse packShipment(Long shipmentId, Long requestVersion, Long currentUserId) {
		LockedShipment locked = findOrderAndShipmentForUpdate(shipmentId);
		Shipment shipment = locked.shipment();
		validateVersion(shipment, requestVersion);
		validateShipmentStatus(shipment, ShipmentStatus.PENDING, "포장 대기 중인 출고만 포장을 확정할 수 있습니다.");
		validateOrderRegistered(locked.order());
		validateActiveWarehouse(shipment.getWarehouse());
		List<SalesOrderItem> orderItems = findOrderItems(locked.order().getSalesOrderId());
		List<ShipmentLot> shipmentLots = shipmentLotRepository.findAllByShipmentIdForUpdate(shipmentId);
		validateStoredPacking(shipment, orderItems, shipmentLots, true);
		lockAndValidateLatestInventory(shipment, shipmentLots, false);
		Customer customer = findCustomerForUpdate(locked.order().getCustomer().getCustomerId());
		validateCustomerForShipment(customer);
		AppUser currentUser = findUser(currentUserId);

		for (ShipmentLot shipmentLot : shipmentLots) {
			inventoryService.reserveForShipment(shipmentLot.getInventoryLot().getInventoryLotId(),
					shipmentLot.getPackedQuantity());
			shipmentLot.reserve();
		}
		shipment.pack(currentUser);
		DeliveryNote deliveryNote = deliveryNoteRepository.save(
				DeliveryNote.create(shipment, shipment.getPackingSequence(), currentUser));
		flushShipmentChanges();

		return new ShipmentPackResponse(shipment.getShipmentId(), shipment.getStatus(), shipment.getPackingSequence(),
				deliveryNote.getDeliveryNoteId(), deliveryNote.getIssueSequence(), shipment.getVersion());
	}

	// ========== PACKED 출고의 LOT 예약을 모두 해제하고 납품서를 무효화한 뒤 PENDING으로 되돌리는 메서드 ==========
	@Transactional
	public ShipmentUnpackResponse unpackShipment(Long shipmentId, ShipmentUnpackRequest request,
			Long currentUserId) {
		LockedShipment locked = findOrderAndShipmentForUpdate(shipmentId);
		Shipment shipment = locked.shipment();
		validateVersion(shipment, request.version());
		validateShipmentStatus(shipment, ShipmentStatus.PACKED, "포장 완료된 출고만 포장을 취소할 수 있습니다.");
		validateOrderRegistered(locked.order());
		List<ShipmentLot> reservedLots = shipmentLotRepository.findReservedByShipmentIdForUpdate(shipmentId);
		validateReservedPackingExists(reservedLots);
		inventoryService.getInventoryLotsForUpdate(reservedLots.stream()
				.map(lot -> lot.getInventoryLot().getInventoryLotId()).distinct().sorted().toList());
		AppUser currentUser = findUser(currentUserId);
		BigDecimal releasedQuantity = BigDecimal.ZERO;

		for (ShipmentLot shipmentLot : reservedLots) {
			inventoryService.releaseShipmentReservation(shipmentLot.getInventoryLot().getInventoryLotId(),
					shipmentLot.getPackedQuantity());
			shipmentLot.releaseReservation();
			releasedQuantity = releasedQuantity.add(shipmentLot.getPackedQuantity());
		}
		deliveryNoteRepository.findAllByShipmentIdAndStatusForUpdate(shipmentId, DeliveryNoteStatus.ACTIVE)
				.forEach(note -> note.voidNote(currentUser, request.reason().trim()));
		shipment.unpack();
		flushShipmentChanges();

		return new ShipmentUnpackResponse(shipment.getShipmentId(), shipment.getStatus(), reservedLots.size(),
				releasedQuantity, shipment.getVersion());
	}

	// ========== PACKED 출고를 재고·변동 이력·매출 전표·미수금·주문과 함께 한 트랜잭션으로 완료하는 메서드 ==========
	// 주문 → 출고 → 재고 LOT → 거래처 → 전표 순서로 잠가 주문 취소 및 정산 업무와 잠금 순서를 통일한다.
	@Transactional
	public ShipmentCompleteResponse completeShipment(Long shipmentId, Long requestVersion, Long currentUserId) {
		LockedShipment locked = findOrderAndShipmentForUpdate(shipmentId);
		Shipment shipment = locked.shipment();
		validateVersion(shipment, requestVersion);
		validateShipmentStatus(shipment, ShipmentStatus.PACKED, "포장 완료된 출고만 실제 출고를 완료할 수 있습니다.");
		validateOrderRegistered(locked.order());
		validateActiveWarehouse(shipment.getWarehouse());
		List<SalesOrderItem> orderItems = findOrderItems(locked.order().getSalesOrderId());
		List<ShipmentLot> shipmentLots = shipmentLotRepository.findAllByShipmentIdForUpdate(shipmentId);
		validateStoredPacking(shipment, orderItems, shipmentLots, true);
		lockAndValidateLatestInventory(shipment, shipmentLots, true);
		Customer customer = findCustomerForUpdate(locked.order().getCustomer().getCustomerId());
		validateCustomerForShipment(customer);
		AppUser currentUser = findUser(currentUserId);

		for (ShipmentLot shipmentLot : shipmentLots) {
			inventoryService.completeShipment(shipmentLot.getInventoryLot().getInventoryLotId(),
					shipmentLot.getPackedQuantity(), shipmentLot.getShipmentLotId(), currentUser);
			// 실제 출고 완료로 INVENTORY_LOT의 예약 수량도 함께 감소했으므로 활성 예약 표시를 해제한다.
			shipmentLot.releaseReservation();
		}
		List<VoucherItemInput> voucherItems = orderItems.stream().map(item ->
				new VoucherItemInput(item.getItem(), item.getOrderQuantity(), item.getUnitPrice())).toList();
		Voucher voucher = settlementService.createSalesVoucher(customer.getCustomerId(), LocalDate.now(),
				shipment.getShipmentId(), voucherItems, currentUser);
		locked.order().complete();
		shipment.complete(currentUser);
		flushShipmentChanges();
		salesOrderRepository.flush();

		return new ShipmentCompleteResponse(shipment.getShipmentId(), shipment.getStatus(),
				locked.order().getSalesOrderId(), locked.order().getStatus(), voucher.getVoucherId(),
				voucher.getTotalAmount(), voucher.getAllocatedAmount(), voucher.getOutstandingAmount(),
				voucher.getSettlementStatus(), shipment.getVersion());
	}

	// ========== 요청 포장안을 주문 품목·창고·LOT 관계와 수량 합계 기준으로 검증하는 메서드 ==========
	private ValidatedPacking validatePackingRequest(List<ShipmentLotAllocationRequest> requests,
			List<SalesOrderItem> orderItems, Warehouse warehouse, boolean requireFullQuantity) {
		Map<Long, SalesOrderItem> orderItemById = orderItems.stream()
				.collect(Collectors.toMap(SalesOrderItem::getSalesOrderItemId, Function.identity()));
		Set<Long> inventoryLotIds = new HashSet<>();
		for (ShipmentLotAllocationRequest request : requests) {
			if (!orderItemById.containsKey(request.salesOrderItemId())) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "현재 주문에 포함되지 않은 품목은 포장할 수 없습니다.");
			}
			if (!inventoryLotIds.add(request.inventoryLotId())) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 재고 LOT를 포장안에 중복 입력할 수 없습니다.");
			}
		}

		Map<Long, InventoryLot> inventoryLotById = inventoryService.getInventoryLotsForUpdate(inventoryLotIds).stream()
				.collect(Collectors.toMap(InventoryLot::getInventoryLotId, Function.identity(),
						(first, second) -> first, LinkedHashMap::new));
		Map<Long, BigDecimal> quantities = new LinkedHashMap<>();
		List<ValidatedPackingInput> inputs = requests.stream().map(request -> {
			SalesOrderItem orderItem = orderItemById.get(request.salesOrderItemId());
			InventoryLot inventoryLot = inventoryLotById.get(request.inventoryLotId());
			validatePackingLot(warehouse, orderItem, inventoryLot, request.packedQuantity());
			quantities.merge(orderItem.getSalesOrderItemId(), request.packedQuantity(), BigDecimal::add);
			return new ValidatedPackingInput(orderItem, inventoryLot, request.packedQuantity());
		}).toList();
		validatePackingQuantityTotals(orderItems, quantities, requireFullQuantity);
		return new ValidatedPacking(inputs);
	}

	private void validatePackingLot(Warehouse warehouse, SalesOrderItem orderItem, InventoryLot inventoryLot,
			BigDecimal quantity) {
		if (!inventoryLot.getWarehouse().getWarehouseId().equals(warehouse.getWarehouseId())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "선택한 출고 창고에 없는 재고 LOT입니다.");
		}
		if (!inventoryLot.getItem().getItemId().equals(orderItem.getItem().getItemId())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "주문 품목과 재고 LOT의 품목이 일치하지 않습니다.");
		}
		inventoryService.validateLotForOutbound(inventoryLot);
		if (inventoryLot.calculateAvailableQuantity().compareTo(quantity) < 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "포장 수량이 재고 LOT의 최신 가용 수량을 초과합니다.");
		}
	}

	// ========== 저장된 포장안의 주문 품목 관계·중복·수량 합계·예약 상태를 다시 검증하는 메서드 ==========
	private void validateStoredPacking(Shipment shipment, List<SalesOrderItem> orderItems,
			List<ShipmentLot> shipmentLots, boolean requireFullQuantity) {
		if (shipment.getWarehouse() == null || shipmentLots.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "포장 확정 전에 출고 창고와 LOT별 포장 수량을 저장해 주세요.");
		}
		Map<Long, SalesOrderItem> orderItemById = orderItems.stream()
				.collect(Collectors.toMap(SalesOrderItem::getSalesOrderItemId, Function.identity()));
		Map<Long, BigDecimal> quantities = new LinkedHashMap<>();
		Set<Long> inventoryLotIds = new HashSet<>();
		for (ShipmentLot shipmentLot : shipmentLots) {
			Long orderItemId = shipmentLot.getSalesOrderItem().getSalesOrderItemId();
			if (!orderItemById.containsKey(orderItemId) || !inventoryLotIds.add(
					shipmentLot.getInventoryLot().getInventoryLotId())) {
				throw new BusinessException(ErrorCode.CONFLICT, "저장된 포장안의 품목 또는 LOT 구성이 올바르지 않습니다.");
			}
			quantities.merge(orderItemId, shipmentLot.getPackedQuantity(), BigDecimal::add);
		}
		validatePackingQuantityTotals(orderItems, quantities, requireFullQuantity);
	}

	private void validatePackingQuantityTotals(List<SalesOrderItem> orderItems, Map<Long, BigDecimal> quantities,
			boolean requireFullQuantity) {
		for (SalesOrderItem orderItem : orderItems) {
			BigDecimal packed = quantities.getOrDefault(orderItem.getSalesOrderItemId(), BigDecimal.ZERO);
			int comparison = packed.compareTo(orderItem.getOrderQuantity());
			if (comparison > 0) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "품목별 포장 수량은 주문 수량을 초과할 수 없습니다.");
			}
			if (requireFullQuantity && comparison != 0) {
				throw new BusinessException(ErrorCode.CONFLICT, "모든 주문 품목의 포장 수량이 주문 수량과 일치해야 합니다.");
			}
		}
	}

	// ========== 저장 포장 LOT를 고정 순서로 잠근 최신 재고와 비교하여 예약 또는 완료 가능 여부를 검증하는 메서드 ==========
	private void lockAndValidateLatestInventory(Shipment shipment, List<ShipmentLot> shipmentLots,
			boolean requireReserved) {
		Map<Long, InventoryLot> lockedLots = inventoryService.getInventoryLotsForUpdate(shipmentLots.stream()
				.map(lot -> lot.getInventoryLot().getInventoryLotId()).distinct().sorted().toList()).stream()
				.collect(Collectors.toMap(InventoryLot::getInventoryLotId, Function.identity()));
		for (ShipmentLot shipmentLot : shipmentLots) {
			InventoryLot inventoryLot = lockedLots.get(shipmentLot.getInventoryLot().getInventoryLotId());
			if (!inventoryLot.getWarehouse().getWarehouseId().equals(shipment.getWarehouse().getWarehouseId())
					|| !inventoryLot.getItem().getItemId().equals(shipmentLot.getSalesOrderItem().getItem().getItemId())) {
				throw new BusinessException(ErrorCode.CONFLICT, "저장된 출고 창고·품목과 재고 LOT 정보가 일치하지 않습니다.");
			}
			inventoryService.validateLotForOutbound(inventoryLot);
			if (requireReserved) {
				if (!shipmentLot.isReserved()
						|| inventoryLot.getReservedQuantity().compareTo(shipmentLot.getPackedQuantity()) < 0) {
					throw new BusinessException(ErrorCode.CONFLICT, "포장 수량에 대한 최신 재고 예약을 확인할 수 없습니다.");
				}
			} else if (shipmentLot.isReserved()
					|| inventoryLot.calculateAvailableQuantity().compareTo(shipmentLot.getPackedQuantity()) < 0) {
				throw new BusinessException(ErrorCode.CONFLICT, "재고 LOT의 최신 가용 수량이 포장 수량보다 부족합니다.");
			}
		}
	}

	private ShipmentDetailResponse createDetailResponse(Shipment shipment, UserRole role) {
		List<SalesOrderItem> orderItems = findOrderItems(shipment.getSalesOrder().getSalesOrderId());
		List<ShipmentLot> shipmentLots = shipmentLotRepository.findAllByShipmentIdWithDetails(shipment.getShipmentId());
		List<DeliveryNote> deliveryNotes = deliveryNoteRepository.findAllByShipmentIdWithUsers(shipment.getShipmentId());
		return ShipmentDetailResponse.from(shipment, orderItems, shipmentLots, deliveryNotes, role,
				findRestrictedInventoryLotIds(shipmentLots));
	}

	// ========== 상세 화면에 표시할 현재 포장 LOT 중 실사 또는 미처리 조정으로 제한된 LOT 식별자를 조회하는 메서드 ==========
	private Set<Long> findRestrictedInventoryLotIds(List<ShipmentLot> shipmentLots) {
		return shipmentLots.stream().map(ShipmentLot::getInventoryLot).filter(inventoryService::hasInventoryWorkRestriction)
				.map(InventoryLot::getInventoryLotId).collect(Collectors.toSet());
	}

	// 주문 취소와 같은 주문 → 출고 잠금 순서를 유지하기 위해 출고 식별자로 주문을 확인한 뒤 주문부터 잠근다.
	private LockedShipment findOrderAndShipmentForUpdate(Long shipmentId) {
		Shipment snapshot = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "출고를 찾을 수 없습니다."));
		SalesOrder order = salesOrderRepository.findByIdForUpdate(snapshot.getSalesOrder().getSalesOrderId())
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "출고의 주문을 찾을 수 없습니다."));
		Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "출고를 찾을 수 없습니다."));
		if (!shipment.getSalesOrder().getSalesOrderId().equals(order.getSalesOrderId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "출고와 주문 연결 정보가 일치하지 않습니다.");
		}
		return new LockedShipment(order, shipment);
	}

	private Shipment findShipmentDetail(Long shipmentId) {
		return shipmentRepository.findDetailById(shipmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "출고를 찾을 수 없습니다."));
	}

	private List<SalesOrderItem> findOrderItems(Long salesOrderId) {
		List<SalesOrderItem> orderItems = salesOrderItemRepository.findAllBySalesOrderIdWithItem(salesOrderId);
		if (orderItems.isEmpty()) throw new BusinessException(ErrorCode.CONFLICT, "출고할 주문 품목을 찾을 수 없습니다.");
		return orderItems;
	}

	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "처리 사용자를 찾을 수 없습니다."));
	}

	private Customer findCustomerForUpdate(Long customerId) {
		return customerRepository.findByIdForUpdate(customerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "거래처를 찾을 수 없습니다."));
	}

	private Warehouse findActiveWarehouse(Long warehouseId) {
		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "창고를 찾을 수 없습니다."));
		validateActiveWarehouse(warehouse);
		return warehouse;
	}

	private Warehouse findActiveWarehouseForUpdate(Long warehouseId) {
		Warehouse warehouse = warehouseRepository.findByIdForUpdate(warehouseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "창고를 찾을 수 없습니다."));
		validateActiveWarehouse(warehouse);
		return warehouse;
	}

	private void validateActiveWarehouse(Warehouse warehouse) {
		if (warehouse == null || warehouse.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 창고만 출고에 사용할 수 있습니다.");
		}
	}

	private void validateCustomerForShipment(Customer customer) {
		if (customer.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중지된 거래처의 출고는 처리할 수 없습니다.");
		}
		if (customer.getTradeStatus() == CustomerTradeStatus.HOLD) {
			throw new BusinessException(ErrorCode.CONFLICT, "거래 중지 상태의 거래처 출고는 처리할 수 없습니다.");
		}
	}

	private void validateOrderRegistered(SalesOrder order) {
		if (order.getStatus() != SalesOrderStatus.REGISTERED) {
			throw new BusinessException(ErrorCode.CONFLICT, "접수 상태의 주문에 연결된 출고만 처리할 수 있습니다.");
		}
	}

	private void validateShipmentStatus(Shipment shipment, ShipmentStatus expected, String message) {
		if (shipment.getStatus() != expected) throw new BusinessException(ErrorCode.CONFLICT, message);
	}

	private void validateReservedPackingExists(List<ShipmentLot> reservedLots) {
		if (reservedLots.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "해제할 출고 LOT 예약을 찾을 수 없습니다.");
		}
	}

	private void validateVersion(Shipment shipment, Long requestVersion) {
		if (requestVersion == null || !requestVersion.equals(shipment.getVersion())) {
			throw createVersionConflictException();
		}
	}

	private void validatePage(int page) {
		if (page < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 늦을 수 없습니다.");
		}
	}

	// ========== UPDATE를 즉시 실행하여 출고의 최종 낙관적 잠금 충돌을 현재 요청 안에서 확인하는 메서드 ==========
	private void flushShipmentChanges() {
		try {
			shipmentRepository.flush();
		} catch (OptimisticLockingFailureException exception) {
			throw createVersionConflictException();
		}
	}

	private BusinessException createVersionConflictException() {
		return new BusinessException(ErrorCode.CONFLICT,
				"다른 사용자가 먼저 출고를 처리했습니다. 최신 출고 정보를 다시 조회해 주세요.");
	}

	private record LockedShipment(SalesOrder order, Shipment shipment) {
	}

	private record ValidatedPacking(List<ValidatedPackingInput> inputs) {
	}

	private record ValidatedPackingInput(SalesOrderItem orderItem, InventoryLot inventoryLot, BigDecimal quantity) {
	}
}
