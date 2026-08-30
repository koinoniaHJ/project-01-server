package com.erp.server.purchase.returning.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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
import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.inventory.service.InventoryService;
import com.erp.server.purchase.receipt.domain.Receipt;
import com.erp.server.purchase.receipt.domain.ReceiptLot;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;
import com.erp.server.purchase.receipt.repository.ReceiptLotRepository;
import com.erp.server.purchase.receipt.repository.ReceiptRepository;
import com.erp.server.purchase.returning.domain.PurchaseReturn;
import com.erp.server.purchase.returning.domain.PurchaseReturnItem;
import com.erp.server.purchase.returning.domain.PurchaseReturnStatus;
import com.erp.server.purchase.returning.dto.PurchaseReturnCancelRequest;
import com.erp.server.purchase.returning.dto.PurchaseReturnCompleteResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnCreateRequest;
import com.erp.server.purchase.returning.dto.PurchaseReturnDetailResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnItemRequest;
import com.erp.server.purchase.returning.dto.PurchaseReturnItemResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnListResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnSourceItemResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnSourceResponse;
import com.erp.server.purchase.returning.dto.PurchaseReturnUpdateRequest;
import com.erp.server.purchase.returning.repository.PurchaseReturnItemRepository;
import com.erp.server.purchase.returning.repository.PurchaseReturnItemRepository.CompletedReturnQuantityProjection;
import com.erp.server.purchase.returning.repository.PurchaseReturnRepository;
import com.erp.server.settlement.domain.Voucher;
import com.erp.server.settlement.repository.VoucherRepository;
import com.erp.server.settlement.service.SettlementService;
import com.erp.server.settlement.service.VoucherItemInput;

import lombok.RequiredArgsConstructor;

// ********** 매입 반품 목록·등록·수정·완료·취소와 원본 입고 LOT·재고·매입 반품 전표 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseReturnService {

	private static final int PURCHASE_RETURN_PAGE_SIZE = 20;
	private static final int QUANTITY_SCALE = 3;

	private final AppUserRepository appUserRepository;
	private final ReceiptRepository receiptRepository;
	private final ReceiptLotRepository receiptLotRepository;
	private final PurchaseReturnRepository purchaseReturnRepository;
	private final PurchaseReturnItemRepository purchaseReturnItemRepository;
	private final InventoryService inventoryService;
	private final VoucherRepository voucherRepository;
	private final SettlementService settlementService;

	// ========== 원본 입고·상태 조건으로 매입 반품 목록을 최신 등록 순서로 페이지 조회하는 메서드 ==========
	public Page<PurchaseReturnListResponse> getPurchaseReturns(Long receiptId, PurchaseReturnStatus status,
			int page, UserRole userRole) {
		validatePage(page);
		PageRequest pageable = PageRequest.of(page, PURCHASE_RETURN_PAGE_SIZE,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("purchaseReturnId")));

		return purchaseReturnRepository.findAllByFilters(receiptId, status, pageable)
				.map(purchaseReturn -> PurchaseReturnListResponse.from(purchaseReturn, userRole));
	}

	// ========== 완료 입고의 LOT별 원본·완료 반품·미예약 재고·반품 가능 수량을 신규 등록 기준으로 조회하는 메서드 ==========
	public PurchaseReturnSourceResponse getPurchaseReturnSource(Long receiptId, UserRole userRole) {
		Receipt receipt = findCompletedReceipt(receiptId);
		List<ReceiptLot> receiptLots = findReceiptLots(receiptId);
		Map<Long, BigDecimal> completedQuantities = findCompletedReturnQuantities(receiptLots);
		boolean warehouse = userRole == UserRole.WAREHOUSE;

		List<PurchaseReturnSourceItemResponse> items = receiptLots.stream()
				.map(receiptLot -> createSourceItemResponse(receiptLot, completedQuantities, warehouse))
				.toList();

		return new PurchaseReturnSourceResponse(receipt.getReceiptId(),
				receipt.getPurchaseOrder().getPurchaseOrderId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierCode(),
				receipt.getPurchaseOrder().getSupplier().getSupplierName(), receipt.getWarehouse().getWarehouseId(),
				receipt.getWarehouse().getWarehouseCode(), receipt.getWarehouse().getWarehouseName(), items);
	}

	// ========== purchaseReturnId로 매입 반품·원본 입고·LOT별 수량·금액·처리 이력·전표를 상세 조회하는 메서드 ==========
	public PurchaseReturnDetailResponse getPurchaseReturn(Long purchaseReturnId, UserRole userRole) {
		PurchaseReturn purchaseReturn = findPurchaseReturnDetail(purchaseReturnId);
		return createDetailResponse(purchaseReturn, userRole);
	}

	// ========== 완료 입고의 원본 LOT별 반품 가능 수량과 현재 미예약 재고를 검증하여 REGISTERED 반품을 생성하는 메서드 ==========
	@Transactional
	public PurchaseReturnDetailResponse createPurchaseReturn(PurchaseReturnCreateRequest request,
			Long currentUserId, UserRole userRole) {
		Receipt receipt = findCompletedReceipt(request.receiptId());
		validateOriginalPurchaseVoucherExists(receipt.getReceiptId());
		List<ReceiptLot> receiptLots = findReceiptLots(receipt.getReceiptId());
		List<ValidatedReturnInput> inputs = validateReturnInputs(receiptLots, request.items());
		AppUser currentUser = findUser(currentUserId);
		PurchaseReturn purchaseReturn = PurchaseReturn.create(receipt, requireReason(request.reason()), currentUser);

		addPurchaseReturnItems(purchaseReturn, inputs);
		PurchaseReturn savedPurchaseReturn = savePurchaseReturn(purchaseReturn);
		return createDetailResponse(savedPurchaseReturn, savedPurchaseReturn.getItems(), userRole);
	}

	// ========== REGISTERED 상태와 version을 검증하고 반품 LOT별 수량·사유를 전체 교체하는 메서드 ==========
	@Transactional
	public PurchaseReturnDetailResponse updatePurchaseReturn(Long purchaseReturnId,
			PurchaseReturnUpdateRequest request, UserRole userRole) {
		PurchaseReturn purchaseReturn = findPurchaseReturnForUpdate(purchaseReturnId);
		validateVersion(purchaseReturn, request.version());
		validateRegisteredStatus(purchaseReturn, "반품 등록 상태에서만 매입 반품을 수정할 수 있습니다.");

		Receipt receipt = purchaseReturn.getReceipt();
		List<ReceiptLot> receiptLots = findReceiptLots(receipt.getReceiptId());
		List<ValidatedReturnInput> inputs = validateReturnInputs(receiptLots, request.items());

		purchaseReturn.updateReason(requireReason(request.reason()));
		purchaseReturn.clearItems();
		purchaseReturnRepository.flush();
		addPurchaseReturnItems(purchaseReturn, inputs);
		flushPurchaseReturnChanges();

		return createDetailResponse(purchaseReturn, purchaseReturn.getItems(), userRole);
	}

	// ========== REGISTERED 반품을 LOT 재고 감소·PURCHASE_RETURN 변동 이력·마이너스 매입 전표와 함께 완료하는 메서드 ==========
	// 매입 반품 행을 잠근 뒤 여러 재고 LOT를 식별자 오름차순으로 잠가 완료 누계와 최신 미예약 수량을 다시 검증한다.
	@Transactional
	public PurchaseReturnCompleteResponse completePurchaseReturn(Long purchaseReturnId, Long requestVersion,
			Long currentUserId) {
		PurchaseReturn purchaseReturn = findPurchaseReturnForUpdate(purchaseReturnId);
		validateVersion(purchaseReturn, requestVersion);
		validateRegisteredStatus(purchaseReturn, "반품 등록 상태에서만 매입 반품을 완료할 수 있습니다.");

		List<PurchaseReturnItem> returnItems = findPurchaseReturnItems(purchaseReturnId);
		validateReturnItemsExist(returnItems);
		Map<Long, InventoryLot> lockedInventoryLots = lockInventoryLots(returnItems);
		Map<Long, BigDecimal> completedQuantities = findCompletedReturnQuantitiesByIds(
				returnItems.stream().map(item -> item.getReceiptLot().getReceiptLotId()).toList());
		validateCompletionQuantities(purchaseReturn, returnItems, lockedInventoryLots, completedQuantities);

		AppUser currentUser = findUser(currentUserId);
		for (PurchaseReturnItem item : returnItems) {
			inventoryService.decreaseForPurchaseReturn(item.getInventoryLot().getInventoryLotId(),
					item.getReturnQuantity(), item.getPurchaseReturnItemId(), currentUser);
		}

		Voucher purchaseReturnVoucher = createPurchaseReturnVoucher(purchaseReturn, returnItems);
		purchaseReturn.complete(currentUser);
		flushPurchaseReturnChanges();

		return new PurchaseReturnCompleteResponse(purchaseReturn.getPurchaseReturnId(), purchaseReturn.getStatus(),
				purchaseReturnVoucher.getVoucherId(), purchaseReturn.getVersion());
	}

	// ========== REGISTERED 상태와 version을 검증하고 사유를 기록하여 매입 반품을 취소하는 메서드 ==========
	@Transactional
	public PurchaseReturnDetailResponse cancelPurchaseReturn(Long purchaseReturnId,
			PurchaseReturnCancelRequest request, Long currentUserId, UserRole userRole) {
		PurchaseReturn purchaseReturn = findPurchaseReturnForUpdate(purchaseReturnId);
		validateVersion(purchaseReturn, request.version());
		validateRegisteredStatus(purchaseReturn, "반품 등록 상태에서만 매입 반품을 취소할 수 있습니다.");

		AppUser currentUser = findUser(currentUserId);
		purchaseReturn.cancel(currentUser, requireReason(request.reason()));
		flushPurchaseReturnChanges();

		return createDetailResponse(purchaseReturn, userRole);
	}

	// ========== 요청 LOT가 완료 입고에 속하고 중복되지 않으며 원본·재고 가능 수량을 초과하지 않는지 검증하는 메서드 ==========
	private List<ValidatedReturnInput> validateReturnInputs(List<ReceiptLot> receiptLots,
			List<PurchaseReturnItemRequest> requests) {
		Map<Long, ReceiptLot> receiptLotById = receiptLots.stream()
				.collect(Collectors.toMap(ReceiptLot::getReceiptLotId, Function.identity()));
		Map<Long, BigDecimal> completedQuantities = findCompletedReturnQuantities(receiptLots);
		Set<Long> requestedReceiptLotIds = new HashSet<>();

		return requests.stream().map(request -> {
			if (!requestedReceiptLotIds.add(request.receiptLotId())) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 입고 LOT를 반품 품목에 중복 입력할 수 없습니다.");
			}

			ReceiptLot receiptLot = receiptLotById.get(request.receiptLotId());
			if (receiptLot == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "원본 완료 입고에 포함되지 않은 LOT는 반품할 수 없습니다.");
			}

			validatePositiveQuantity(request.returnQuantity(), "반품 수량");
			validateReceiptLotInventoryConnection(receiptLot);
			validateReturnableQuantity(receiptLot, request.returnQuantity(),
					completedQuantities.getOrDefault(receiptLot.getReceiptLotId(), BigDecimal.ZERO),
					receiptLot.getInventoryLot());

			return new ValidatedReturnInput(receiptLot, request.returnQuantity());
		}).toList();
	}

	// ========== 검증된 원본 LOT 입력을 원본 발주 단가가 고정된 반품 품목으로 생성하여 상위 반품에 추가하는 메서드 ==========
	private void addPurchaseReturnItems(PurchaseReturn purchaseReturn, List<ValidatedReturnInput> inputs) {
		for (ValidatedReturnInput input : inputs) {
			BigDecimal unitPrice = input.receiptLot().getReceiptItem().getPurchaseOrderItem().getUnitPrice();
			purchaseReturn.addItem(PurchaseReturnItem.create(purchaseReturn, input.receiptLot(),
					input.returnQuantity(), unitPrice));
		}
	}

	// ========== 여러 반품 품목의 재고 LOT를 중복 제거한 식별자 고정 순서로 잠그고 ID Map으로 변환하는 메서드 ==========
	private Map<Long, InventoryLot> lockInventoryLots(List<PurchaseReturnItem> returnItems) {
		List<Long> inventoryLotIds = returnItems.stream().map(PurchaseReturnItem::getInventoryLot)
				.map(InventoryLot::getInventoryLotId).distinct().toList();

		return inventoryService.getInventoryLotsForUpdate(inventoryLotIds).stream()
				.collect(Collectors.toMap(InventoryLot::getInventoryLotId, Function.identity(),
						(first, second) -> first, LinkedHashMap::new));
	}

	// ========== 완료 직전 원본 입고·재고 LOT 연결·누적 완료 반품·최신 미예약 재고를 잠금 값으로 다시 검증하는 메서드 ==========
	private void validateCompletionQuantities(PurchaseReturn purchaseReturn, List<PurchaseReturnItem> returnItems,
			Map<Long, InventoryLot> lockedInventoryLots, Map<Long, BigDecimal> completedQuantities) {
		if (purchaseReturn.getReceipt().getStatus() != ReceiptStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.CONFLICT, "검수 완료된 원본 입고의 품목만 반품할 수 있습니다.");
		}

		for (PurchaseReturnItem item : returnItems) {
			ReceiptLot receiptLot = item.getReceiptLot();
			if (!Objects.equals(receiptLot.getReceiptItem().getReceipt().getReceiptId(),
					purchaseReturn.getReceipt().getReceiptId())) {
				throw new BusinessException(ErrorCode.CONFLICT, "매입 반품 품목의 원본 입고 정보가 일치하지 않습니다.");
			}

			InventoryLot lockedInventoryLot = lockedInventoryLots.get(item.getInventoryLot().getInventoryLotId());
			if (lockedInventoryLot == null
					|| !Objects.equals(receiptLot.getInventoryLot().getInventoryLotId(),
							lockedInventoryLot.getInventoryLotId())) {
				throw new BusinessException(ErrorCode.CONFLICT, "원본 입고 LOT와 재고 LOT 연결이 일치하지 않습니다.");
			}

			validateReturnableQuantity(receiptLot, item.getReturnQuantity(),
					completedQuantities.getOrDefault(receiptLot.getReceiptLotId(), BigDecimal.ZERO),
					lockedInventoryLot);
		}
	}

	// ========== 요청 수량이 원본 입고 잔여 반품 가능 수량과 최신 미예약 현재 재고를 모두 초과하지 않는지 검증하는 메서드 ==========
	private void validateReturnableQuantity(ReceiptLot receiptLot, BigDecimal requestedQuantity,
			BigDecimal completedReturnQuantity, InventoryLot inventoryLot) {
		BigDecimal originalRemainingQuantity = receiptLot.getNormalQuantity().subtract(completedReturnQuantity);
		BigDecimal availableQuantity = inventoryLot.calculateAvailableQuantity();

		if (originalRemainingQuantity.signum() < 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "원본 입고 LOT의 완료 반품 누계가 정상 입고 수량을 초과했습니다.");
		}
		if (requestedQuantity.compareTo(originalRemainingQuantity) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "반품 수량이 원본 입고 LOT의 잔여 반품 가능 수량을 초과합니다.");
		}
		if (requestedQuantity.compareTo(availableQuantity) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "반품 수량이 재고 LOT의 최신 미예약 보유 수량을 초과합니다.");
		}
	}

	// ========== 완료 반품 품목을 원본 발주 단가의 전표 입력으로 변환하여 PURCHASE_RETURN 전표를 생성하는 메서드 ==========
	private Voucher createPurchaseReturnVoucher(PurchaseReturn purchaseReturn,
			List<PurchaseReturnItem> returnItems) {
		Voucher originalPurchaseVoucher = voucherRepository.findByReceiptId(purchaseReturn.getReceipt().getReceiptId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
						"원본 입고의 매입 전표를 찾을 수 없어 매입 반품을 완료할 수 없습니다."));
		List<VoucherItemInput> voucherItems = returnItems.stream()
				.map(item -> new VoucherItemInput(item.getItem(), item.getReturnQuantity(), item.getUnitPrice()))
				.toList();

		return settlementService.createPurchaseReturnVoucher(originalPurchaseVoucher.getVoucherId(), LocalDate.now(),
				purchaseReturn.getPurchaseReturnId(), voucherItems);
	}

	// ========== 매입 반품 상세 응답에 필요한 품목을 조회하여 역할별 금액과 최신 가능 수량을 함께 구성하는 메서드 ==========
	private PurchaseReturnDetailResponse createDetailResponse(PurchaseReturn purchaseReturn, UserRole userRole) {
		List<PurchaseReturnItem> items = findPurchaseReturnItems(purchaseReturn.getPurchaseReturnId());
		return createDetailResponse(purchaseReturn, items, userRole);
	}

	// ========== 전달받은 반품 품목으로 매입 반품 상세 응답을 생성하는 메서드 ==========
	private PurchaseReturnDetailResponse createDetailResponse(PurchaseReturn purchaseReturn,
			List<PurchaseReturnItem> items, UserRole userRole) {
		List<Long> receiptLotIds = items.stream().map(item -> item.getReceiptLot().getReceiptLotId()).toList();
		Map<Long, BigDecimal> completedQuantities = findCompletedReturnQuantitiesByIds(receiptLotIds);
		boolean warehouse = userRole == UserRole.WAREHOUSE;

		List<PurchaseReturnItemResponse> itemResponses = items.stream()
				.map(item -> createItemResponse(item, completedQuantities, warehouse)).toList();
		Voucher voucher = voucherRepository.findByPurchaseReturnId(purchaseReturn.getPurchaseReturnId()).orElse(null);
		Receipt receipt = purchaseReturn.getReceipt();

		return new PurchaseReturnDetailResponse(purchaseReturn.getPurchaseReturnId(), receipt.getReceiptId(),
				receipt.getPurchaseOrder().getPurchaseOrderId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierId(),
				receipt.getPurchaseOrder().getSupplier().getSupplierCode(),
				receipt.getPurchaseOrder().getSupplier().getSupplierName(), receipt.getWarehouse().getWarehouseId(),
				receipt.getWarehouse().getWarehouseCode(), receipt.getWarehouse().getWarehouseName(),
				purchaseReturn.getStatus(), purchaseReturn.getReason(),
				warehouse ? null : purchaseReturn.getTotalAmount(), itemResponses,
				com.erp.server.purchase.returning.dto.PurchaseReturnActionResponse.from(
						purchaseReturn.getCreatedBy(), purchaseReturn.getCreatedAt()),
				com.erp.server.purchase.returning.dto.PurchaseReturnActionResponse.from(
						purchaseReturn.getCompletedBy(), purchaseReturn.getCompletedAt()),
				com.erp.server.purchase.returning.dto.PurchaseReturnActionResponse.from(
						purchaseReturn.getCanceledBy(), purchaseReturn.getCanceledAt()),
				purchaseReturn.getCancelReason(), voucher == null ? null : voucher.getVoucherId(),
				purchaseReturn.getUpdatedAt(), purchaseReturn.getVersion());
	}

	// ========== 원본 입고 LOT를 신규 반품 등록 화면의 가능 수량 응답으로 변환하는 메서드 ==========
	private PurchaseReturnSourceItemResponse createSourceItemResponse(ReceiptLot receiptLot,
			Map<Long, BigDecimal> completedQuantities, boolean warehouse) {
		BigDecimal completedQuantity = completedQuantities.getOrDefault(receiptLot.getReceiptLotId(), BigDecimal.ZERO);
		InventoryLot inventoryLot = receiptLot.getInventoryLot();
		BigDecimal availableQuantity = inventoryLot.calculateAvailableQuantity();
		BigDecimal originalRemainingQuantity = receiptLot.getNormalQuantity().subtract(completedQuantity)
				.max(BigDecimal.ZERO);
		BigDecimal returnableQuantity = originalRemainingQuantity.min(availableQuantity.max(BigDecimal.ZERO));
		var purchaseOrderItem = receiptLot.getReceiptItem().getPurchaseOrderItem();
		var item = purchaseOrderItem.getItem();

		return new PurchaseReturnSourceItemResponse(receiptLot.getReceiptLotId(), inventoryLot.getInventoryLotId(),
				purchaseOrderItem.getLineNo(), item.getItemId(), item.getItemCode(), item.getItemName(), item.getUnit(),
				item.getOtherUnitName(), receiptLot.getSupplierLotNumber(), receiptLot.getLotNumber(),
				receiptLot.getExpiryDate(), receiptLot.getNormalQuantity(), completedQuantity,
				inventoryLot.getCurrentQuantity(), inventoryLot.getReservedQuantity(), availableQuantity,
				returnableQuantity, warehouse ? null : purchaseOrderItem.getUnitPrice());
	}

	// ========== 저장된 반품 품목을 상세 화면의 역할별 금액·최신 가능 수량 응답으로 변환하는 메서드 ==========
	private PurchaseReturnItemResponse createItemResponse(PurchaseReturnItem returnItem,
			Map<Long, BigDecimal> completedQuantities, boolean warehouse) {
		ReceiptLot receiptLot = returnItem.getReceiptLot();
		InventoryLot inventoryLot = returnItem.getInventoryLot();
		BigDecimal completedQuantity = completedQuantities.getOrDefault(receiptLot.getReceiptLotId(), BigDecimal.ZERO);
		BigDecimal availableQuantity = inventoryLot.calculateAvailableQuantity();
		BigDecimal originalRemainingQuantity = receiptLot.getNormalQuantity().subtract(completedQuantity)
				.max(BigDecimal.ZERO);
		BigDecimal returnableQuantity = originalRemainingQuantity.min(availableQuantity.max(BigDecimal.ZERO));
		var purchaseOrderItem = receiptLot.getReceiptItem().getPurchaseOrderItem();
		var item = returnItem.getItem();

		return new PurchaseReturnItemResponse(returnItem.getPurchaseReturnItemId(), receiptLot.getReceiptLotId(),
				inventoryLot.getInventoryLotId(), purchaseOrderItem.getLineNo(), item.getItemId(), item.getItemCode(),
				item.getItemName(), item.getUnit(), item.getOtherUnitName(), receiptLot.getSupplierLotNumber(),
				receiptLot.getLotNumber(), receiptLot.getExpiryDate(), receiptLot.getNormalQuantity(), completedQuantity,
				inventoryLot.getCurrentQuantity(), inventoryLot.getReservedQuantity(), availableQuantity,
				returnableQuantity, returnItem.getReturnQuantity(), warehouse ? null : returnItem.getUnitPrice(),
				warehouse ? null : returnItem.getLineAmount());
	}

	// ========== 원본 입고 LOT 목록의 완료 반품 누적 수량을 ID Map으로 조회하는 메서드 ==========
	private Map<Long, BigDecimal> findCompletedReturnQuantities(List<ReceiptLot> receiptLots) {
		return findCompletedReturnQuantitiesByIds(
				receiptLots.stream().map(ReceiptLot::getReceiptLotId).toList());
	}

	// ========== 원본 입고 LOT 식별자별 COMPLETED 반품 누적 수량 조회 결과를 Map으로 변환하는 메서드 ==========
	private Map<Long, BigDecimal> findCompletedReturnQuantitiesByIds(List<Long> receiptLotIds) {
		if (receiptLotIds.isEmpty()) {
			return Map.of();
		}

		return purchaseReturnItemRepository.sumCompletedQuantitiesByReceiptLotIds(receiptLotIds).stream()
				.collect(Collectors.toMap(CompletedReturnQuantityProjection::getReceiptLotId,
						CompletedReturnQuantityProjection::getReturnedQuantity));
	}

	// ========== receiptId로 완료 입고와 상세 연관정보를 조회하고 없거나 완료 전이면 업무 오류를 발생시키는 메서드 ==========
	private Receipt findCompletedReceipt(Long receiptId) {
		Receipt receipt = receiptRepository.findDetailById(receiptId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "원본 입고를 찾을 수 없습니다."));

		if (receipt.getStatus() != ReceiptStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.CONFLICT, "검수 완료된 입고만 매입 반품의 원본으로 선택할 수 있습니다.");
		}
		return receipt;
	}

	// ========== 완료 입고의 정상 수량·재고 연결이 있는 원본 LOT를 조회하는 메서드 ==========
	private List<ReceiptLot> findReceiptLots(Long receiptId) {
		List<ReceiptLot> receiptLots = receiptLotRepository.findAllReturnableByReceiptId(receiptId);

		if (receiptLots.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "반품할 수 있는 정상 입고 LOT가 없습니다.");
		}
		return receiptLots;
	}

	// ========== purchaseReturnId로 처리 이력까지 포함한 매입 반품을 조회하는 메서드 ==========
	private PurchaseReturn findPurchaseReturnDetail(Long purchaseReturnId) {
		return purchaseReturnRepository.findDetailById(purchaseReturnId)
				.orElseThrow(() -> createPurchaseReturnNotFoundException());
	}

	// ========== purchaseReturnId로 매입 반품을 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	private PurchaseReturn findPurchaseReturnForUpdate(Long purchaseReturnId) {
		return purchaseReturnRepository.findByIdForUpdate(purchaseReturnId)
				.orElseThrow(() -> createPurchaseReturnNotFoundException());
	}

	// ========== 한 매입 반품의 LOT별 품목 상세를 조회하는 메서드 ==========
	private List<PurchaseReturnItem> findPurchaseReturnItems(Long purchaseReturnId) {
		return purchaseReturnItemRepository.findAllByPurchaseReturnIdWithDetails(purchaseReturnId);
	}

	// ========== userId로 처리 사용자를 조회하는 메서드 ==========
	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "처리 사용자를 찾을 수 없습니다."));
	}

	// ========== 원본 입고에 PURCHASE 전표가 존재하는지 등록 전에 확인하는 메서드 ==========
	private void validateOriginalPurchaseVoucherExists(Long receiptId) {
		if (voucherRepository.findByReceiptId(receiptId).isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "매입 전표가 생성된 완료 입고만 매입 반품을 등록할 수 있습니다.");
		}
	}

	// ========== 원본 입고 LOT가 검수 완료 시 생성 또는 재사용된 재고 LOT와 연결되어 있는지 확인하는 메서드 ==========
	private void validateReceiptLotInventoryConnection(ReceiptLot receiptLot) {
		if (receiptLot.getInventoryLot() == null) {
			throw new BusinessException(ErrorCode.CONFLICT, "재고 LOT가 연결되지 않은 입고 LOT는 반품할 수 없습니다.");
		}
	}

	// ========== 완료 처리할 반품 품목이 하나 이상 존재하는지 확인하는 메서드 ==========
	private void validateReturnItemsExist(List<PurchaseReturnItem> returnItems) {
		if (returnItems.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "반품 품목이 없어 매입 반품을 완료할 수 없습니다.");
		}
	}

	// ========== 매입 반품이 수정·완료·취소 가능한 REGISTERED 상태인지 확인하는 메서드 ==========
	private void validateRegisteredStatus(PurchaseReturn purchaseReturn, String message) {
		if (purchaseReturn.getStatus() != PurchaseReturnStatus.REGISTERED) {
			throw new BusinessException(ErrorCode.CONFLICT, message);
		}
	}

	// ========== 요청 version과 잠금 조회한 최신 매입 반품 version이 같은지 검증하는 메서드 ==========
	private void validateVersion(PurchaseReturn purchaseReturn, Long requestVersion) {
		if (!Objects.equals(purchaseReturn.getVersion(), requestVersion)) {
			throw createVersionConflictException();
		}
	}

	// ========== 반품 수량이 0보다 크고 소수점 셋째 자리 이내인지 검증하는 메서드 ==========
	private void validatePositiveQuantity(BigDecimal quantity, String fieldName) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "은 0보다 커야 합니다.");
		}
		if (quantity.stripTrailingZeros().scale() > QUANTITY_SCALE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "은 소수점 셋째 자리까지만 입력할 수 있습니다.");
		}
	}

	// ========== 반품 사유의 앞뒤 공백을 제거하고 필수값·길이를 검증하는 메서드 ==========
	private String requireReason(String reason) {
		if (reason == null || reason.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "매입 반품 사유를 입력해 주세요.");
		}

		String normalizedReason = reason.trim();
		if (normalizedReason.length() > 1000) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "매입 반품 사유는 1000자 이하로 입력해 주세요.");
		}
		return normalizedReason;
	}

	// ========== 0부터 시작하는 목록 페이지 번호가 올바른지 검증하는 메서드 ==========
	private void validatePage(int page) {
		if (page < 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
		}
	}

	// ========== 매입 반품과 자식 품목을 저장하고 DB 고유 제약 충돌을 업무 오류로 변환하는 메서드 ==========
	private PurchaseReturn savePurchaseReturn(PurchaseReturn purchaseReturn) {
		try {
			return purchaseReturnRepository.saveAndFlush(purchaseReturn);
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "같은 원본 입고 LOT를 매입 반품 품목에 중복 등록할 수 없습니다.");
		}
	}

	// ========== UPDATE를 즉시 실행하여 최종 낙관적 잠금과 DB 제약 충돌을 현재 요청 안에서 확인하는 메서드 ==========
	private void flushPurchaseReturnChanges() {
		try {
			purchaseReturnRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw createVersionConflictException();
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "매입 반품 처리 중 중복되거나 올바르지 않은 데이터가 확인되었습니다.");
		}
	}

	// ========== 존재하지 않는 매입 반품에 사용할 공통 404 업무 예외를 생성하는 메서드 ==========
	private BusinessException createPurchaseReturnNotFoundException() {
		return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "매입 반품을 찾을 수 없습니다.");
	}

	// ========== version 불일치에 사용할 공통 409 업무 예외를 생성하는 메서드 ==========
	private BusinessException createVersionConflictException() {
		return new BusinessException(ErrorCode.CONFLICT,
				"다른 사용자가 매입 반품 정보를 먼저 변경했습니다. 최신 정보를 다시 조회해 주세요.");
	}

	// 검증이 끝난 원본 입고 LOT와 요청 반품 수량을 순서대로 전달한다.
	private record ValidatedReturnInput(ReceiptLot receiptLot, BigDecimal returnQuantity) {
	}
}
