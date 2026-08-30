package com.erp.server.purchase.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.item.domain.Item;
import com.erp.server.master.item.repository.ItemRepository;
import com.erp.server.master.item.repository.SupplierItemRepository;
import com.erp.server.master.supplier.domain.Supplier;
import com.erp.server.master.supplier.repository.SupplierRepository;
import com.erp.server.purchase.order.domain.PurchaseOrder;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderItem;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.order.dto.PurchaseOrderCreateRequest;
import com.erp.server.purchase.order.dto.PurchaseOrderCancelRequest;
import com.erp.server.purchase.order.dto.PurchaseOrderDetailResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderItemRequest;
import com.erp.server.purchase.order.dto.PurchaseOrderListResponse;
import com.erp.server.purchase.order.dto.PurchaseOrderUpdateRequest;
import com.erp.server.purchase.order.repository.PurchaseOrderItemRepository;
import com.erp.server.purchase.order.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

// ********** 발주 조회·작성·수정·삭제와 승인 요청·승인·발주 확정 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

	private static final int PURCHASE_ORDER_PAGE_SIZE = 20;

	private final AppUserRepository appUserRepository;
	private final SupplierRepository supplierRepository;
	private final ItemRepository itemRepository;
	private final SupplierItemRepository supplierItemRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderItemRepository purchaseOrderItemRepository;

	// ========== 상태·이메일 상태·공급업체·등록 기간 조건을 적용하여 발주 목록을 페이지 조회하는 메서드 ==========
	// 시작일과 종료일은 발주 등록 일시 기준으로 모두 포함하고 모든 선택 조건을 동시에 적용한다.
	// 목록은 페이지당 20건이며 등록 일시 내림차순, 같은 일시면 발주 식별자 내림차순으로 고정한다.
	public Page<PurchaseOrderListResponse> getPurchaseOrders(PurchaseOrderStatus status,
			PurchaseOrderEmailStatus emailStatus, Long supplierId, LocalDate startDate, LocalDate endDate,
			int page, UserRole currentUserRole) {
		validatePage(page);
		validateDateRange(startDate, endDate);

		LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
		LocalDateTime endAtExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
		PageRequest pageable = PageRequest.of(page, PURCHASE_ORDER_PAGE_SIZE,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("purchaseOrderId")));

		return purchaseOrderRepository
				.findAllByFilters(status, emailStatus, supplierId, startAt, endAtExclusive, pageable)
				.map(row -> PurchaseOrderListResponse.from(row, currentUserRole));
	}

	// ========== purchaseOrderId로 발주 기본정보·품목·입고 현황·처리 이력을 상세 조회하는 메서드 ==========
	// WAREHOUSE 역할의 단가·금액·이메일 상태는 DTO 변환 과정에서 null로 반환한다.
	public PurchaseOrderDetailResponse getPurchaseOrder(Long purchaseOrderId, UserRole currentUserRole) {
		PurchaseOrder purchaseOrder = purchaseOrderRepository.findDetailById(purchaseOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "발주를 찾을 수 없습니다."));
		List<PurchaseOrderItem> items = purchaseOrderItemRepository.findAllByPurchaseOrderId(purchaseOrderId);

		return PurchaseOrderDetailResponse.from(purchaseOrder, items, currentUserRole);
	}

	// ========== ACTIVE 공급업체와 취급 품목을 검증하고 DRAFT 발주를 등록하는 메서드 ==========
	@Transactional
	public PurchaseOrderDetailResponse createPurchaseOrder(PurchaseOrderCreateRequest request, Long currentUserId) {
		validateDuplicateItems(request.items());

		AppUser currentUser = findUser(currentUserId);
		Supplier supplier = findSupplierForUpdate(request.supplierId());
		List<Long> itemIds = extractItemIds(request.items());
		List<Item> items = findAndValidateItemsForUpdate(itemIds);

		validateSupplierIsActive(supplier);
		validateSupplierHandlesAllItems(supplier.getSupplierId(), itemIds);

		PurchaseOrder purchaseOrder = PurchaseOrder.create(supplier, normalizeOptionalValue(request.memo()), currentUser);
		addPurchaseOrderItems(purchaseOrder, request.items(), items);

		PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.saveAndFlush(purchaseOrder);

		return createDetailResponse(savedPurchaseOrder, savedPurchaseOrder.getItems(), currentUser.getRole());
	}

	// ========== DRAFT 상태와 version을 검증하고 발주의 공급업체·품목·메모를 수정하는 메서드 ==========
	@Transactional
	public PurchaseOrderDetailResponse updatePurchaseOrder(Long purchaseOrderId, PurchaseOrderUpdateRequest request,
			Long currentUserId) {
		validateDuplicateItems(request.items());

		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(purchaseOrderId);
		validateVersion(purchaseOrder, request.version());
		validateStatus(purchaseOrder, PurchaseOrderStatus.DRAFT, "작성 중인 발주만 수정할 수 있습니다.");

		AppUser currentUser = findUser(currentUserId);
		Supplier supplier = findSupplierForUpdate(request.supplierId());
		List<Long> itemIds = extractItemIds(request.items());
		List<Item> items = findAndValidateItemsForUpdate(itemIds);

		validateSupplierIsActive(supplier);
		validateSupplierHandlesAllItems(supplier.getSupplierId(), itemIds);

		// 기존 자식 행을 먼저 삭제하여 같은 품목과 표시 순번을 다시 사용할 때 UNIQUE 제약조건이 충돌하지 않게 한다.
		purchaseOrderItemRepository.deleteAllByPurchaseOrderId(purchaseOrderId);
		purchaseOrder.clearItems();
		purchaseOrder.updateDraft(supplier, normalizeOptionalValue(request.memo()));
		addPurchaseOrderItems(purchaseOrder, request.items(), items);

		flushPurchaseOrderChanges();

		return createDetailResponse(purchaseOrder, purchaseOrder.getItems(), currentUser.getRole());
	}

	// ========== DRAFT 상태와 version을 검증하고 작성 중 발주와 발주 품목을 물리 삭제하는 메서드 ==========
	@Transactional
	public void deletePurchaseOrder(Long purchaseOrderId, Long requestVersion) {
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(purchaseOrderId);

		validateVersion(purchaseOrder, requestVersion);
		validateStatus(purchaseOrder, PurchaseOrderStatus.DRAFT, "작성 중인 발주만 삭제할 수 있습니다.");

		purchaseOrderRepository.delete(purchaseOrder);
		flushPurchaseOrderChanges();
	}

	// ========== 최신 공급업체·품목·취급 관계를 다시 검증하고 DRAFT 발주를 SUBMITTED로 변경하는 메서드 ==========
	@Transactional
	public PurchaseOrderDetailResponse submitPurchaseOrder(Long purchaseOrderId, Long requestVersion,
			Long currentUserId) {
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(purchaseOrderId);

		validateVersion(purchaseOrder, requestVersion);
		validateStatus(purchaseOrder, PurchaseOrderStatus.DRAFT, "작성 중인 발주만 승인 요청할 수 있습니다.");

		List<PurchaseOrderItem> purchaseOrderItems = findPurchaseOrderItems(purchaseOrderId);
		validateStoredPurchaseOrderReferences(purchaseOrder, purchaseOrderItems);

		AppUser currentUser = findUser(currentUserId);
		purchaseOrder.submit(currentUser);
		flushPurchaseOrderChanges();

		return createDetailResponse(purchaseOrder, purchaseOrderItems, currentUser.getRole());
	}

	// ========== SUBMITTED 상태와 version을 검증하고 발주를 APPROVED로 변경하는 메서드 ==========
	// ADMIN 권한은 이후 Controller와 SecurityConfig에서 최종 제한한다.
	@Transactional
	public PurchaseOrderDetailResponse approvePurchaseOrder(Long purchaseOrderId, Long requestVersion,
			Long currentUserId) {
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(purchaseOrderId);

		validateVersion(purchaseOrder, requestVersion);
		validateStatus(purchaseOrder, PurchaseOrderStatus.SUBMITTED, "승인 대기 발주만 승인할 수 있습니다.");

		AppUser currentUser = findUser(currentUserId);
		purchaseOrder.approve(currentUser);
		flushPurchaseOrderChanges();

		return createDetailResponse(purchaseOrder, findPurchaseOrderItems(purchaseOrderId), currentUser.getRole());
	}

	// ========== 최신 공급업체·품목·취급 관계를 다시 검증하고 APPROVED 발주를 ORDERED로 확정하는 메서드 ==========
	// 이 트랜잭션은 발주 확정 상태를 먼저 DB에 커밋하며 PDF 생성과 SMTP 전송은 다음 단계에서 커밋 이후 별도로 실행한다.
	@Transactional
	public PurchaseOrderDetailResponse orderPurchaseOrder(Long purchaseOrderId, Long requestVersion,
			Long currentUserId) {
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(purchaseOrderId);

		validateVersion(purchaseOrder, requestVersion);
		validateStatus(purchaseOrder, PurchaseOrderStatus.APPROVED, "승인 완료 발주만 공급업체 발주로 확정할 수 있습니다.");

		List<PurchaseOrderItem> purchaseOrderItems = findPurchaseOrderItems(purchaseOrderId);
		validateStoredPurchaseOrderReferences(purchaseOrder, purchaseOrderItems);

		AppUser currentUser = findUser(currentUserId);
		purchaseOrder.order(currentUser);
		flushPurchaseOrderChanges();

		return createDetailResponse(purchaseOrder, purchaseOrderItems, currentUser.getRole());
	}

	// ========== SUBMITTED·APPROVED 또는 취소 조건을 충족한 ORDERED 발주를 CANCELED로 변경하는 메서드 ==========
	// ORDERED 발주는 정상 입고 누계와 진행 중 입고가 없어야 하며 이메일 전송 성공 후에는 공급업체 취소 확인이 필수이다.
	@Transactional
	public PurchaseOrderDetailResponse cancelPurchaseOrder(Long purchaseOrderId, PurchaseOrderCancelRequest request,
			Long currentUserId) {
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(purchaseOrderId);

		validateVersion(purchaseOrder, request.version());
		validateCancelableStatus(purchaseOrder);

		boolean orderedPurchaseOrder = purchaseOrder.getStatus() == PurchaseOrderStatus.ORDERED;

		if (orderedPurchaseOrder) {
			validateOrderedPurchaseOrderCancellation(purchaseOrder, request.supplierCancelConfirmed());
		}

		AppUser currentUser = findUser(currentUserId);
		boolean recordSupplierConfirmation = orderedPurchaseOrder
				&& Boolean.TRUE.equals(request.supplierCancelConfirmed());

		purchaseOrder.cancel(currentUser, request.reason().trim(), recordSupplierConfirmation);
		flushPurchaseOrderChanges();

		return createDetailResponse(purchaseOrder, findPurchaseOrderItems(purchaseOrderId), currentUser.getRole());
	}

	// ========== 현재 발주 상태가 취소 가능한 SUBMITTED·APPROVED·ORDERED인지 검증하는 메서드 ==========
	private void validateCancelableStatus(PurchaseOrder purchaseOrder) {
		PurchaseOrderStatus status = purchaseOrder.getStatus();

		if (status != PurchaseOrderStatus.SUBMITTED
				&& status != PurchaseOrderStatus.APPROVED
				&& status != PurchaseOrderStatus.ORDERED) {
			throw new BusinessException(ErrorCode.CONFLICT, "현재 상태의 발주는 취소할 수 없습니다.");
		}
	}

	// ========== ORDERED 발주의 입고 참조와 이메일 전송 성공 후 공급업체 취소 확인 여부를 검증하는 메서드 ==========
	private void validateOrderedPurchaseOrderCancellation(PurchaseOrder purchaseOrder,
			Boolean supplierCancelConfirmed) {
		long blockingReferenceCount = purchaseOrderRepository
				.countCancellationBlockingReferences(purchaseOrder.getPurchaseOrderId());

		if (blockingReferenceCount > 0) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"정상 입고 이력 또는 진행 중인 입고가 있는 발주는 취소할 수 없습니다.");
		}

		if (purchaseOrder.getEmailStatus() == PurchaseOrderEmailStatus.SENT
				&& !Boolean.TRUE.equals(supplierCancelConfirmed)) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"이메일 전송이 완료된 발주는 공급업체와 취소를 확인한 후 취소할 수 있습니다.");
		}
	}

	// ========== 저장된 발주의 공급업체·품목·취급 관계가 현재도 유효한지 검증하는 메서드 ==========
	private void validateStoredPurchaseOrderReferences(PurchaseOrder purchaseOrder,
			List<PurchaseOrderItem> purchaseOrderItems) {
		if (purchaseOrderItems.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "발주 품목이 없어 처리할 수 없습니다.");
		}

		Supplier supplier = findSupplierForUpdate(purchaseOrder.getSupplier().getSupplierId());
		List<Long> itemIds = purchaseOrderItems.stream()
				.map(item -> item.getItem().getItemId())
				.sorted()
				.toList();

		findAndValidateItemsForUpdate(itemIds);
		validateSupplierIsActive(supplier);
		validateSupplierHandlesAllItems(supplier.getSupplierId(), itemIds);
	}

	// ========== 요청 순서를 lineNo로 사용하여 발주 품목 Entity를 생성하고 상위 발주에 추가하는 메서드 ==========
	private void addPurchaseOrderItems(PurchaseOrder purchaseOrder, List<PurchaseOrderItemRequest> requests,
			List<Item> items) {
		Map<Long, Item> itemById = items.stream()
				.collect(Collectors.toMap(Item::getItemId, Function.identity()));

		for (int index = 0; index < requests.size(); index++) {
			PurchaseOrderItemRequest request = requests.get(index);
			PurchaseOrderItem purchaseOrderItem = PurchaseOrderItem.create(purchaseOrder, index + 1,
					itemById.get(request.itemId()), request.orderedQuantity(), request.unitPrice());

			purchaseOrder.addItem(purchaseOrderItem);
		}
	}

	// ========== 요청 품목 식별자 목록을 추출하고 비관적 잠금으로 조회하여 존재 여부와 ACTIVE 상태를 검증하는 메서드 ==========
	private List<Item> findAndValidateItemsForUpdate(List<Long> itemIds) {
		List<Item> items = itemRepository.findAllByIdsForUpdate(itemIds);

		if (items.size() != itemIds.size()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "일부 발주 품목을 찾을 수 없습니다.");
		}

		boolean inactiveItemExists = items.stream().anyMatch(item -> item.getStatus() != MasterStatus.ACTIVE);

		if (inactiveItemExists) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 품목만 발주할 수 있습니다.");
		}

		return items;
	}

	// ========== 한 발주 요청 안에 같은 품목이 중복 포함되지 않았는지 검증하는 메서드 ==========
	private void validateDuplicateItems(List<PurchaseOrderItemRequest> requests) {
		List<Long> itemIds = extractItemIds(requests);

		if (new HashSet<>(itemIds).size() != itemIds.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 품목을 발주에 중복으로 추가할 수 없습니다.");
		}
	}

	// ========== 발주 품목 요청에서 itemId를 오름차순으로 추출하여 잠금 순서를 고정하는 메서드 ==========
	private List<Long> extractItemIds(List<PurchaseOrderItemRequest> requests) {
		return requests.stream().map(PurchaseOrderItemRequest::itemId).sorted().toList();
	}

	// ========== 공급업체가 현재 ACTIVE 상태인지 검증하는 메서드 ==========
	private void validateSupplierIsActive(Supplier supplier) {
		if (supplier.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 공급업체만 발주할 수 있습니다.");
		}
	}

	// ========== 선택한 공급업체와 모든 발주 품목의 SUPPLIER_ITEM 취급 관계가 존재하는지 검증하는 메서드 ==========
	private void validateSupplierHandlesAllItems(Long supplierId, List<Long> itemIds) {
		long handledItemCount = supplierItemRepository.countBySupplierIdAndItemIds(supplierId, itemIds);

		if (handledItemCount != itemIds.size()) {
			throw new BusinessException(ErrorCode.CONFLICT, "선택한 공급업체가 취급하지 않는 품목이 포함되어 있습니다.");
		}
	}

	// ========== 발주가 요청 처리에 필요한 현재 상태인지 검증하는 메서드 ==========
	private void validateStatus(PurchaseOrder purchaseOrder, PurchaseOrderStatus requiredStatus, String message) {
		if (purchaseOrder.getStatus() != requiredStatus) {
			throw new BusinessException(ErrorCode.CONFLICT, message);
		}
	}

	// ========== 요청 version과 잠금 조회한 최신 발주 version이 같은지 검증하는 메서드 ==========
	private void validateVersion(PurchaseOrder purchaseOrder, Long requestVersion) {
		if (!Objects.equals(purchaseOrder.getVersion(), requestVersion)) {
			throw createVersionConflictException();
		}
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== 발주 업무 중 공급업체 상태 변경을 막기 위해 supplierId로 비관적 잠금 조회하는 메서드 ==========
	private Supplier findSupplierForUpdate(Long supplierId) {
		return supplierRepository.findByIdForUpdate(supplierId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "공급업체를 찾을 수 없습니다."));
	}

	// ========== 발주 수정·삭제·상태 전이 중 같은 발주의 동시 처리를 막기 위해 비관적 잠금 조회하는 메서드 ==========
	private PurchaseOrder findPurchaseOrderForUpdate(Long purchaseOrderId) {
		return purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "발주를 찾을 수 없습니다."));
	}

	// ========== 발주 품목과 품목 기본정보를 lineNo 오름차순으로 조회하는 메서드 ==========
	private List<PurchaseOrderItem> findPurchaseOrderItems(Long purchaseOrderId) {
		return purchaseOrderItemRepository.findAllByPurchaseOrderId(purchaseOrderId);
	}

	// ========== Entity와 발주 품목을 현재 사용자 역할에 맞는 상세 응답으로 변환하는 메서드 ==========
	private PurchaseOrderDetailResponse createDetailResponse(PurchaseOrder purchaseOrder,
			List<PurchaseOrderItem> items, UserRole currentUserRole) {
		return PurchaseOrderDetailResponse.from(purchaseOrder, items, currentUserRole);
	}

	// ========== 발주 INSERT·UPDATE·DELETE를 즉시 실행하여 마지막 저장 순간의 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushPurchaseOrderChanges() {
		try {
			purchaseOrderRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw createVersionConflictException();
		}
	}

	// ========== 발주 동시 수정·상태 전이 충돌에 사용할 409 업무 예외를 생성하는 메서드 ==========
	private BusinessException createVersionConflictException() {
		return new BusinessException(ErrorCode.CONFLICT,
				"다른 사용자가 먼저 발주를 수정하거나 처리했습니다. 최신 발주 정보를 다시 조회해 주세요.");
	}

	// ========== 선택 문자열의 앞뒤 공백을 제거하고 값이 없으면 null로 변환하는 메서드 ==========
	private String normalizeOptionalValue(String value) {
		if (value == null) {
			return null;
		}

		String normalizedValue = value.trim();

		return normalizedValue.isEmpty() ? null : normalizedValue;
	}

	// ========== 페이지 번호가 0 이상인지 검증하는 메서드 ==========
	private void validatePage(int page) {
		if (page < 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
		}
	}

	// ========== 시작일이 종료일보다 늦지 않은 올바른 등록 기간인지 검증하는 메서드 ==========
	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 늦을 수 없습니다.");
		}
	}
}
