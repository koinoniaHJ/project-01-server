package com.erp.server.purchase.receipt.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.erp.server.common.user.repository.AppUserRepository;
import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.inventory.repository.InventoryLotRepository;
import com.erp.server.inventory.service.InventoryService;
import com.erp.server.master.common.domain.MasterStatus;
import com.erp.server.master.supplier.domain.Supplier;
import com.erp.server.master.warehouse.domain.Warehouse;
import com.erp.server.master.warehouse.repository.WarehouseRepository;
import com.erp.server.purchase.order.domain.PurchaseOrder;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderItem;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.order.repository.PurchaseOrderItemRepository;
import com.erp.server.purchase.order.repository.PurchaseOrderRepository;
import com.erp.server.purchase.receipt.domain.Receipt;
import com.erp.server.purchase.receipt.domain.ReceiptItem;
import com.erp.server.purchase.receipt.domain.ReceiptLot;
import com.erp.server.purchase.receipt.domain.ReceiptRemainderAction;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;
import com.erp.server.purchase.receipt.dto.ReceiptCancelRequest;
import com.erp.server.purchase.receipt.dto.ReceiptCompleteRequest;
import com.erp.server.purchase.receipt.dto.ReceiptCompleteResponse;
import com.erp.server.purchase.receipt.dto.ReceiptCreateRequest;
import com.erp.server.purchase.receipt.dto.ReceiptDetailResponse;
import com.erp.server.purchase.receipt.dto.ReceiptInspectionItemRequest;
import com.erp.server.purchase.receipt.dto.ReceiptInspectionLotRequest;
import com.erp.server.purchase.receipt.dto.ReceiptInspectionRequest;
import com.erp.server.purchase.receipt.dto.ReceiptListResponse;
import com.erp.server.purchase.receipt.dto.ReceiptWarehouseUpdateRequest;
import com.erp.server.purchase.receipt.repository.ReceiptItemRepository;
import com.erp.server.purchase.receipt.repository.ReceiptRepository;
import com.erp.server.settlement.domain.Voucher;
import com.erp.server.settlement.repository.VoucherRepository;
import com.erp.server.settlement.service.SettlementService;
import com.erp.server.settlement.service.VoucherItemInput;

import lombok.RequiredArgsConstructor;

// ********** 입고 목록·등록·검수 저장·완료·취소와 발주·재고·매입 전표 연계 업무 규칙을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptService {

	private static final int RECEIPT_PAGE_SIZE = 20;
	private static final int QUANTITY_SCALE = 3;

	private final AppUserRepository appUserRepository;
	private final WarehouseRepository warehouseRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PurchaseOrderItemRepository purchaseOrderItemRepository;
	private final ReceiptRepository receiptRepository;
	private final ReceiptItemRepository receiptItemRepository;
	private final InventoryLotRepository inventoryLotRepository;
	private final InventoryService inventoryService;
	private final VoucherRepository voucherRepository;
	private final SettlementService settlementService;

	// ========== 발주·검수 상태·등록 기간 조건을 적용하여 입고 목록을 페이지 조회하는 메서드 ==========
	// 시작일과 종료일은 입고 등록 일시 기준으로 모두 포함하고 페이지당 20건을 최신 등록 순서로 반환한다.
	public Page<ReceiptListResponse> getReceipts(Long purchaseOrderId, ReceiptStatus status, LocalDate startDate,
			LocalDate endDate, int page) {
		validatePage(page);
		validateDateRange(startDate, endDate);
		LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
		LocalDateTime endAtExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
		PageRequest pageable = PageRequest.of(page, RECEIPT_PAGE_SIZE,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("receiptId")));

		return receiptRepository.findAllByFilters(purchaseOrderId, status, startAt, endAtExclusive, pageable)
				.map(ReceiptListResponse::from);
	}

	// ========== receiptId로 입고·발주·창고·검수 품목·LOT·매입 전표를 상세 조회하는 메서드 ==========
	public ReceiptDetailResponse getReceipt(Long receiptId) {
		Receipt receipt = receiptRepository.findDetailById(receiptId)
				.orElseThrow(() -> createReceiptNotFoundException());

		return createDetailResponse(receipt);
	}

	// ========== ORDERED 발주와 ACTIVE 창고를 검증하고 발주 품목 전체를 포함한 PENDING 입고를 등록하는 메서드 ==========
	@Transactional
	public ReceiptDetailResponse createReceipt(ReceiptCreateRequest request, Long currentUserId) {
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(request.purchaseOrderId());
		validatePurchaseOrderCanReceive(purchaseOrder);

		List<PurchaseOrderItem> purchaseOrderItems = findPurchaseOrderItems(purchaseOrder.getPurchaseOrderId());
		validateRemainingQuantityExists(purchaseOrderItems);

		Warehouse warehouse = findActiveWarehouseForUpdate(request.warehouseId());
		AppUser currentUser = findUser(currentUserId);
		Receipt receipt = Receipt.create(purchaseOrder, warehouse, currentUser);

		for (PurchaseOrderItem purchaseOrderItem : purchaseOrderItems) {
			receipt.addItem(ReceiptItem.create(receipt, purchaseOrderItem));
		}

		Receipt savedReceipt = receiptRepository.saveAndFlush(receipt);
		return createDetailResponse(savedReceipt, savedReceipt.getItems());
	}

	// ========== PENDING 상태와 version을 검증하고 입고 반영 창고를 ACTIVE 창고로 변경하는 메서드 ==========
	@Transactional
	public ReceiptDetailResponse updateWarehouse(Long receiptId, ReceiptWarehouseUpdateRequest request) {
		Receipt receipt = findReceiptForUpdate(receiptId);
		validateVersion(receipt, request.version());
		validateStatus(receipt, ReceiptStatus.PENDING, "검수 대기 입고만 창고를 변경할 수 있습니다.");

		Warehouse warehouse = findActiveWarehouseForUpdate(request.warehouseId());
		receipt.changeWarehouse(warehouse);
		flushReceiptChanges();

		return createDetailResponse(receipt);
	}

	// ========== PENDING 상태와 version을 검증하고 입고를 INSPECTING으로 변경하는 메서드 ==========
	@Transactional
	public ReceiptDetailResponse startInspection(Long receiptId, Long requestVersion, Long currentUserId) {
		Receipt receipt = findReceiptForUpdate(receiptId);
		validateVersion(receipt, requestVersion);
		validateStatus(receipt, ReceiptStatus.PENDING, "검수 대기 입고만 검수를 시작할 수 있습니다.");

		AppUser currentUser = findUser(currentUserId);
		receipt.startInspection(currentUser);
		flushReceiptChanges();

		return createDetailResponse(receipt);
	}

	// ========== INSPECTING 입고의 모든 품목 수량·메모·LOT 구성을 검증하고 전체 교체 저장하는 메서드 ==========
	@Transactional
	public ReceiptDetailResponse saveInspection(Long receiptId, ReceiptInspectionRequest request) {
		Receipt receipt = findReceiptForUpdate(receiptId);
		validateVersion(receipt, request.version());
		validateStatus(receipt, ReceiptStatus.INSPECTING, "검수 중인 입고만 검수 결과를 저장할 수 있습니다.");

		List<ReceiptItem> receiptItems = findReceiptItems(receiptId);
		Map<Long, ReceiptInspectionItemRequest> requestByReceiptItemId = validateInspectionRequest(
				receiptItems, request.items());

		for (ReceiptItem receiptItem : receiptItems) {
			ReceiptInspectionItemRequest itemRequest = requestByReceiptItemId.get(receiptItem.getReceiptItemId());
			applyInspection(receiptItem, itemRequest);
		}

		receipt.markInspectionSaved();
		flushReceiptChanges();
		return createDetailResponse(receipt, receiptItems);
	}

	// ========== 검수 완료를 발주·재고 LOT·재고 변동·매입 전표·입고 상태에 하나의 트랜잭션으로 반영하는 메서드 ==========
	// PURCHASE_ORDER를 먼저 잠근 뒤 RECEIPT와 기존 INVENTORY_LOT를 고정 순서로 잠가 최신 잔여 수량과 동시 처리를 재검증한다.
	@Transactional
	public ReceiptCompleteResponse completeReceipt(Long receiptId, ReceiptCompleteRequest request,
			Long currentUserId) {
		Receipt receiptSnapshot = receiptRepository.findById(receiptId)
				.orElseThrow(() -> createReceiptNotFoundException());
		PurchaseOrder purchaseOrder = findPurchaseOrderForUpdate(
				receiptSnapshot.getPurchaseOrder().getPurchaseOrderId());
		Receipt receipt = findReceiptForUpdate(receiptId);

		validateVersion(receipt, request.version());
		validateStatus(receipt, ReceiptStatus.INSPECTING, "검수 중인 입고만 검수를 완료할 수 있습니다.");
		validatePurchaseOrderCanReceive(purchaseOrder);

		List<ReceiptItem> receiptItems = findReceiptItems(receiptId);
		validateStoredInspection(receiptItems);

		BigDecimal cumulativeReceivedAfter = calculateCumulativeReceivedAfter(receiptItems);
		BigDecimal totalOrderedQuantity = calculateTotalOrderedQuantity(receiptItems);
		BigDecimal totalActualQuantity = calculateTotalActualQuantity(receiptItems);
		boolean fullyReceived = cumulativeReceivedAfter.compareTo(totalOrderedQuantity) == 0;
		CompletionDecision completionDecision = validateCompletionDecision(request, cumulativeReceivedAfter,
				totalOrderedQuantity, totalActualQuantity, fullyReceived);
		validatePurchaseOrderCancellation(purchaseOrder, receiptId, completionDecision);

		AppUser currentUser = findUser(currentUserId);
		List<InventoryTarget> inventoryTargets = prepareInventoryTargets(receipt, receiptItems);
		lockExistingInventoryLots(inventoryTargets);
		applyInventory(receiptItems, inventoryTargets, currentUser);
		applyReceivedQuantities(receiptItems);

		Voucher purchaseVoucher = createPurchaseVoucherIfNeeded(receipt, receiptItems);
		updatePurchaseOrderStatus(purchaseOrder, fullyReceived, completionDecision, currentUser);
		receipt.complete(completionDecision.remainderAction(), completionDecision.remainderReason(), currentUser);
		flushReceiptChanges();

		return new ReceiptCompleteResponse(receipt.getReceiptId(), receipt.getStatus(),
				purchaseOrder.getPurchaseOrderId(), purchaseOrder.getStatus(),
				purchaseVoucher == null ? null : purchaseVoucher.getVoucherId(), receipt.getVersion());
	}

	// ========== PENDING 또는 INSPECTING 상태와 version을 검증하고 사유를 기록하여 입고 검수를 취소하는 메서드 ==========
	@Transactional
	public ReceiptDetailResponse cancelReceipt(Long receiptId, ReceiptCancelRequest request, Long currentUserId) {
		Receipt receipt = findReceiptForUpdate(receiptId);
		validateVersion(receipt, request.version());
		validateCancelableStatus(receipt);

		AppUser currentUser = findUser(currentUserId);
		receipt.cancel(currentUser, requireReason(request.reason(), "검수 취소 사유"));
		flushReceiptChanges();

		return createDetailResponse(receipt);
	}

	// ========== 품목별 검수 요청이 저장된 입고 품목과 정확히 일치하고 중복되지 않는지 검증하는 메서드 ==========
	private Map<Long, ReceiptInspectionItemRequest> validateInspectionRequest(List<ReceiptItem> receiptItems,
			List<ReceiptInspectionItemRequest> requests) {
		Map<Long, ReceiptInspectionItemRequest> requestById = new LinkedHashMap<>();

		for (ReceiptInspectionItemRequest request : requests) {
			if (requestById.put(request.receiptItemId(), request) != null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 입고 품목의 검수 결과를 중복 저장할 수 없습니다.");
			}
		}

		Set<Long> storedIds = receiptItems.stream().map(ReceiptItem::getReceiptItemId).collect(Collectors.toSet());
		if (requestById.size() != storedIds.size() || !storedIds.equals(requestById.keySet())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "입고의 모든 품목 검수 결과를 빠짐없이 전달해 주세요.");
		}

		return requestById;
	}

	// ========== 한 입고 품목의 수량 관계·최신 발주 잔여·LOT 합계를 검증하고 Entity에 적용하는 메서드 ==========
	private void applyInspection(ReceiptItem receiptItem, ReceiptInspectionItemRequest request) {
		validateInspectionQuantities(receiptItem.getPurchaseOrderItem(), request.actualQuantity(),
				request.normalQuantity(), request.rejectedQuantity());
		List<NormalizedLotInput> lotInputs = validateAndNormalizeLots(request.normalQuantity(), request.lots());

		receiptItem.replaceInspection(request.actualQuantity(), request.normalQuantity(), request.rejectedQuantity(),
				normalizeOptionalValue(request.note()));
		for (NormalizedLotInput lotInput : lotInputs) {
			receiptItem.addLot(ReceiptLot.create(receiptItem, lotInput.supplierLotNumber(), lotInput.expiryDate(),
					lotInput.normalQuantity()));
		}
	}

	// ========== 완료 직전 저장된 검수 수량과 LOT 합계를 최신 발주 잔여 수량 기준으로 다시 검증하는 메서드 ==========
	private void validateStoredInspection(List<ReceiptItem> receiptItems) {
		if (receiptItems.isEmpty()) {
			throw new BusinessException(ErrorCode.CONFLICT, "입고 품목이 없어 검수를 완료할 수 없습니다.");
		}

		for (ReceiptItem receiptItem : receiptItems) {
			validateInspectionQuantities(receiptItem.getPurchaseOrderItem(), receiptItem.getActualQuantity(),
					receiptItem.getNormalQuantity(), receiptItem.getRejectedQuantity());
			BigDecimal lotQuantity = receiptItem.getLots().stream().map(ReceiptLot::getNormalQuantity)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			if (lotQuantity.compareTo(receiptItem.getNormalQuantity()) != 0) {
				throw new BusinessException(ErrorCode.CONFLICT,
						"품목별 LOT 정상 수량 합계가 저장된 정상 입고 수량과 일치하지 않습니다.");
			}

			if (receiptItem.getNormalQuantity().signum() > 0 && receiptItem.getLots().isEmpty()) {
				throw new BusinessException(ErrorCode.CONFLICT, "정상 입고 수량이 있는 품목에는 LOT가 하나 이상 필요합니다.");
			}
		}
	}

	// ========== 실제 수량=정상+불합격 관계와 정상 수량의 실제·최신 발주 잔여 초과 여부를 검증하는 메서드 ==========
	private void validateInspectionQuantities(PurchaseOrderItem purchaseOrderItem, BigDecimal actualQuantity,
			BigDecimal normalQuantity, BigDecimal rejectedQuantity) {
		validateQuantity(actualQuantity, "실제 입고 수량");
		validateQuantity(normalQuantity, "정상 입고 수량");
		validateQuantity(rejectedQuantity, "불합격 수량");

		if (actualQuantity.compareTo(normalQuantity.add(rejectedQuantity)) != 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"실제 입고 수량은 정상 입고 수량과 불합격 수량의 합과 일치해야 합니다.");
		}

		if (normalQuantity.compareTo(actualQuantity) > 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "정상 입고 수량은 실제 입고 수량을 초과할 수 없습니다.");
		}

		if (normalQuantity.compareTo(purchaseOrderItem.calculateRemainingQuantity()) > 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "정상 입고 수량이 발주 품목의 최신 잔여 수량을 초과합니다.");
		}
	}

	// ========== LOT 필수 여부·수량·중복·합계를 검증하고 정규화된 LOT 입력 목록을 반환하는 메서드 ==========
	private List<NormalizedLotInput> validateAndNormalizeLots(BigDecimal normalQuantity,
			List<ReceiptInspectionLotRequest> lotRequests) {
		if (normalQuantity.signum() == 0) {
			if (!lotRequests.isEmpty()) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "정상 입고 수량이 0인 품목에는 LOT를 입력할 수 없습니다.");
			}
			return List.of();
		}

		if (lotRequests.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "정상 입고 수량이 있는 품목에는 LOT를 하나 이상 입력해 주세요.");
		}

		List<NormalizedLotInput> lotInputs = new ArrayList<>();
		Set<String> lotKeys = new HashSet<>();
		BigDecimal lotQuantity = BigDecimal.ZERO;

		for (ReceiptInspectionLotRequest request : lotRequests) {
			String supplierLotNumber = normalizeOptionalValue(request.supplierLotNumber());
			validatePositiveQuantity(request.normalQuantity(), "LOT 정상 수량");
			String lotKey = (supplierLotNumber == null ? "<INTERNAL>" : supplierLotNumber) + "|" + request.expiryDate();

			if (!lotKeys.add(lotKey)) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 공급업체 LOT와 사용기한을 중복 입력할 수 없습니다.");
			}

			lotInputs.add(new NormalizedLotInput(supplierLotNumber, request.expiryDate(), request.normalQuantity()));
			lotQuantity = lotQuantity.add(request.normalQuantity());
		}

		if (lotQuantity.compareTo(normalQuantity) != 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "LOT 정상 수량 합계는 품목의 정상 입고 수량과 일치해야 합니다.");
		}

		return lotInputs;
	}

	// ========== 전량·부분·전량 불합격 결과에 따라 잔여 처리 또는 발주 취소 요청값을 검증하는 메서드 ==========
	private CompletionDecision validateCompletionDecision(ReceiptCompleteRequest request,
			BigDecimal cumulativeReceivedQuantity, BigDecimal totalOrderedQuantity,
			BigDecimal totalActualQuantity, boolean fullyReceived) {
		boolean cancelPurchaseOrder = Boolean.TRUE.equals(request.cancelPurchaseOrder());
		boolean supplierCancelConfirmed = Boolean.TRUE.equals(request.supplierCancelConfirmed());
		String cancelReason = normalizeOptionalValue(request.cancelReason());
		boolean cancellationInputExists = cancelPurchaseOrder || cancelReason != null || supplierCancelConfirmed;

		if (fullyReceived) {
			if (request.remainderAction() != null || normalizeOptionalValue(request.remainderReason()) != null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "전량 정상 입고된 발주에는 잔여 수량 처리 값을 입력할 수 없습니다.");
			}
			if (cancellationInputExists) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "전량 정상 입고된 발주는 검수 완료와 함께 취소할 수 없습니다.");
			}
			return CompletionDecision.received();
		}

		if (cancelPurchaseOrder) {
			if (cumulativeReceivedQuantity.signum() > 0) {
				throw new BusinessException(ErrorCode.CONFLICT,
						"정상 입고 이력이 있는 발주는 검수 완료와 함께 취소할 수 없습니다.");
			}
			if (totalActualQuantity.signum() == 0) {
				throw new BusinessException(ErrorCode.CONFLICT,
						"실제 입고 수량이 0인 경우에는 검수 완료가 아닌 입고 검수 취소로 처리해 주세요.");
			}
			if (request.remainderAction() != null || normalizeOptionalValue(request.remainderReason()) != null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT,
						"전량 불합격 후 발주를 취소할 때는 잔여 수량 처리 값을 함께 입력할 수 없습니다.");
			}
			return CompletionDecision.cancelPurchaseOrder(requireReason(cancelReason, "발주 취소 사유"),
					supplierCancelConfirmed);
		}

		if (cancelReason != null || supplierCancelConfirmed) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"발주 취소 사유와 공급업체 확인 여부는 발주 동시 취소를 선택한 경우에만 입력할 수 있습니다.");
		}
		if (request.remainderAction() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "잔여 수량 처리 방식을 선택해 주세요.");
		}

		String reason = requireReason(request.remainderReason(), "잔여 수량 처리 사유");
		if (cumulativeReceivedQuantity.signum() == 0
				&& request.remainderAction() != ReceiptRemainderAction.ADDITIONAL_RECEIPT) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"누적 정상 입고 수량이 0인 경우에는 추가 입고 예정만 선택할 수 있습니다.");
		}

		if (cumulativeReceivedQuantity.compareTo(totalOrderedQuantity) >= 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "잔여 수량 처리 대상이 아닌 발주입니다.");
		}

		return CompletionDecision.remainder(request.remainderAction(), reason);
	}

	// ========== 전량 불합격 후 발주 동시 취소가 다른 정상 입고·진행 입고와 이메일 확인 조건을 충족하는지 검증하는 메서드 ==========
	private void validatePurchaseOrderCancellation(PurchaseOrder purchaseOrder, Long receiptId,
			CompletionDecision completionDecision) {
		if (!completionDecision.cancelPurchaseOrder()) {
			return;
		}

		long blockingReferenceCount = purchaseOrderRepository
				.countCancellationBlockingReferencesExcludingReceipt(purchaseOrder.getPurchaseOrderId(), receiptId);
		if (blockingReferenceCount > 0) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"기존 정상 입고 이력 또는 다른 진행 중 입고가 있는 발주는 취소할 수 없습니다.");
		}

		if (purchaseOrder.getEmailStatus() == PurchaseOrderEmailStatus.SENT
				&& !completionDecision.supplierCancelConfirmed()) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"이메일 전송이 완료된 발주는 공급업체와 취소를 확인한 후 취소할 수 있습니다.");
		}

		if (purchaseOrder.getEmailStatus() != PurchaseOrderEmailStatus.SENT
				&& completionDecision.supplierCancelConfirmed()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"공급업체 취소 확인은 발주서 이메일 전송이 완료된 발주에만 기록할 수 있습니다.");
		}
	}

	// ========== 저장된 입고 LOT별로 기존 재고 LOT 재사용 또는 신규 생성 대상을 준비하고 사용기한 일치를 검증하는 메서드 ==========
	private List<InventoryTarget> prepareInventoryTargets(Receipt receipt, List<ReceiptItem> receiptItems) {
		List<InventoryTarget> targets = new ArrayList<>();
		Warehouse warehouse = receipt.getWarehouse();
		Supplier supplier = receipt.getPurchaseOrder().getSupplier();

		for (ReceiptItem receiptItem : receiptItems) {
			for (ReceiptLot receiptLot : receiptItem.getLots()) {
				InventoryLot inventoryLot = null;

				if (!receiptLot.isInternalLot()) {
					inventoryLot = inventoryService.findInventoryLot(warehouse.getWarehouseId(),
							receiptItem.getPurchaseOrderItem().getItem().getItemId(), supplier.getSupplierId(),
							receiptLot.getSupplierLotNumber());
					if (inventoryLot != null && !inventoryLot.getExpiryDate().equals(receiptLot.getExpiryDate())) {
						throw new BusinessException(ErrorCode.CONFLICT,
								"동일 창고·품목·공급업체·LOT 번호의 기존 사용기한과 입력 사용기한이 일치하지 않습니다.");
					}
				}

				targets.add(new InventoryTarget(receiptItem, receiptLot, inventoryLot));
			}
		}

		return targets;
	}

	// ========== 재사용할 기존 재고 LOT를 식별자 오름차순의 고정 순서로 미리 잠그는 메서드 ==========
	private void lockExistingInventoryLots(List<InventoryTarget> targets) {
		List<Long> inventoryLotIds = targets.stream().map(InventoryTarget::inventoryLot)
				.filter(Objects::nonNull).map(InventoryLot::getInventoryLotId).distinct().toList();

		if (inventoryLotIds.isEmpty()) {
			return;
		}

		List<InventoryLot> lockedLots = inventoryService.getInventoryLotsForUpdate(inventoryLotIds);
		Map<Long, InventoryLot> lockedById = lockedLots.stream()
				.collect(Collectors.toMap(InventoryLot::getInventoryLotId, Function.identity()));

		for (InventoryTarget target : targets) {
			if (target.inventoryLot() != null) {
				target.replaceInventoryLot(lockedById.get(target.inventoryLot().getInventoryLotId()));
			}
		}
	}

	// ========== 입고 LOT별 재고 LOT를 생성 또는 재사용하고 정상 수량 증가와 RECEIPT 변동 이력을 반영하는 메서드 ==========
	private void applyInventory(List<ReceiptItem> receiptItems, List<InventoryTarget> targets, AppUser currentUser) {
		Map<Long, InventoryTarget> targetByReceiptLotId = targets.stream()
				.collect(Collectors.toMap(target -> target.receiptLot().getReceiptLotId(), Function.identity()));

		for (ReceiptItem receiptItem : receiptItems) {
			for (ReceiptLot receiptLot : receiptItem.getLots()) {
				InventoryTarget target = targetByReceiptLotId.get(receiptLot.getReceiptLotId());
				InventoryLot inventoryLot = target.inventoryLot();

				if (inventoryLot == null) {
					inventoryLot = createInventoryLot(target, currentUser);
					target.replaceInventoryLot(inventoryLot);
				}

				receiptLot.connectInventoryLot(inventoryLot);
				inventoryService.increaseFromReceipt(inventoryLot.getInventoryLotId(), receiptLot.getNormalQuantity(),
						receiptLot.getReceiptLotId(), currentUser);
			}
		}
	}

	// ========== 공급업체 LOT 또는 내부 생성 LOT 조건에 맞는 신규 재고 LOT를 저장하고 UNIQUE 충돌을 업무 오류로 변환하는 메서드 ==========
	private InventoryLot createInventoryLot(InventoryTarget target, AppUser currentUser) {
		Receipt receipt = target.receiptItem().getReceipt();
		PurchaseOrderItem purchaseOrderItem = target.receiptItem().getPurchaseOrderItem();
		ReceiptLot receiptLot = target.receiptLot();
		InventoryLot inventoryLot;

		if (receiptLot.isInternalLot()) {
			String internalLotNumber = inventoryService.generateInternalLotNumber();
			inventoryLot = InventoryLot.createInternalLot(receipt.getWarehouse(), purchaseOrderItem.getItem(),
					receipt.getPurchaseOrder().getSupplier(), internalLotNumber, receiptLot.getExpiryDate(), currentUser);
		} else {
			inventoryLot = InventoryLot.createSupplierLot(receipt.getWarehouse(), purchaseOrderItem.getItem(),
					receipt.getPurchaseOrder().getSupplier(), receiptLot.getSupplierLotNumber(),
					receiptLot.getExpiryDate(), currentUser);
		}

		try {
			return inventoryLotRepository.saveAndFlush(inventoryLot);
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"같은 창고·품목·공급업체·LOT 번호의 재고가 동시에 생성되었습니다. 최신 정보를 다시 확인해 주세요.");
		}
	}

	// ========== 입고 품목의 정상 수량을 원본 발주 품목별 누적 정상 입고 수량에 반영하는 메서드 ==========
	private void applyReceivedQuantities(List<ReceiptItem> receiptItems) {
		for (ReceiptItem receiptItem : receiptItems) {
			if (receiptItem.getNormalQuantity().signum() > 0) {
				receiptItem.getPurchaseOrderItem().addReceivedQuantity(receiptItem.getNormalQuantity());
			}
		}
	}

	// ========== 정상 입고 수량이 있는 품목만 발주 단가로 PURCHASE 전표 품목을 구성하여 매입 전표를 생성하는 메서드 ==========
	private Voucher createPurchaseVoucherIfNeeded(Receipt receipt, List<ReceiptItem> receiptItems) {
		List<VoucherItemInput> voucherItems = receiptItems.stream()
				.filter(receiptItem -> receiptItem.getNormalQuantity().signum() > 0)
				.map(receiptItem -> new VoucherItemInput(receiptItem.getPurchaseOrderItem().getItem(),
						receiptItem.getNormalQuantity(), receiptItem.getPurchaseOrderItem().getUnitPrice()))
				.toList();

		if (voucherItems.isEmpty()) {
			return null;
		}

		return settlementService.createPurchaseVoucher(receipt.getPurchaseOrder().getSupplier().getSupplierId(),
				LocalDate.now(), receipt.getReceiptId(), voucherItems);
	}

	// ========== 전량 입고·추가 입고·잔여 종료·전량 불합격 취소 결정에 따라 원본 발주 상태와 처리 이력을 변경하는 메서드 ==========
	private void updatePurchaseOrderStatus(PurchaseOrder purchaseOrder, boolean fullyReceived,
			CompletionDecision completionDecision, AppUser currentUser) {
		if (fullyReceived) {
			purchaseOrder.completeReceipt();
		} else if (completionDecision.cancelPurchaseOrder()) {
			purchaseOrder.cancel(currentUser, completionDecision.cancelReason(),
					completionDecision.supplierCancelConfirmed());
		} else if (completionDecision.remainderAction() == ReceiptRemainderAction.CLOSE_REMAINDER) {
			purchaseOrder.closeRemainder(currentUser, completionDecision.remainderReason());
		}
	}

	// ========== 이번 입고 정상 수량 반영 후 발주 전체 누적 정상 입고 수량을 계산하는 메서드 ==========
	private BigDecimal calculateCumulativeReceivedAfter(List<ReceiptItem> receiptItems) {
		return receiptItems.stream()
				.map(receiptItem -> receiptItem.getPurchaseOrderItem().getReceivedQuantity()
						.add(receiptItem.getNormalQuantity()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== 입고에 연결된 원본 발주 품목의 전체 발주 수량을 계산하는 메서드 ==========
	private BigDecimal calculateTotalOrderedQuantity(List<ReceiptItem> receiptItems) {
		return receiptItems.stream().map(receiptItem -> receiptItem.getPurchaseOrderItem().getOrderedQuantity())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== 이번 검수에서 실제로 도착하여 확인한 모든 입고 품목의 실제 수량 합계를 계산하는 메서드 ==========
	private BigDecimal calculateTotalActualQuantity(List<ReceiptItem> receiptItems) {
		return receiptItems.stream().map(ReceiptItem::getActualQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ========== 발주가 입고 등록·완료에 필요한 ORDERED 상태인지 검증하는 메서드 ==========
	private void validatePurchaseOrderCanReceive(PurchaseOrder purchaseOrder) {
		if (purchaseOrder.getStatus() != PurchaseOrderStatus.ORDERED) {
			throw new BusinessException(ErrorCode.CONFLICT, "발주 확정 상태의 발주만 입고 처리할 수 있습니다.");
		}
	}

	// ========== 발주 품목 중 정상 입고할 잔여 수량이 하나 이상 존재하는지 검증하는 메서드 ==========
	private void validateRemainingQuantityExists(List<PurchaseOrderItem> purchaseOrderItems) {
		if (purchaseOrderItems.isEmpty() || purchaseOrderItems.stream()
				.noneMatch(item -> item.calculateRemainingQuantity().signum() > 0)) {
			throw new BusinessException(ErrorCode.CONFLICT, "입고할 잔여 수량이 있는 발주만 입고를 등록할 수 있습니다.");
		}
	}

	// ========== 입고가 PENDING 또는 INSPECTING 상태인지 검증하는 메서드 ==========
	private void validateCancelableStatus(Receipt receipt) {
		if (receipt.getStatus() != ReceiptStatus.PENDING && receipt.getStatus() != ReceiptStatus.INSPECTING) {
			throw new BusinessException(ErrorCode.CONFLICT, "검수 대기 또는 검수 중인 입고만 취소할 수 있습니다.");
		}
	}

	// ========== 입고가 요청 처리에 필요한 현재 상태인지 검증하는 메서드 ==========
	private void validateStatus(Receipt receipt, ReceiptStatus requiredStatus, String message) {
		if (receipt.getStatus() != requiredStatus) {
			throw new BusinessException(ErrorCode.CONFLICT, message);
		}
	}

	// ========== 요청 version과 비관적 잠금 조회한 최신 입고 version이 같은지 검증하는 메서드 ==========
	private void validateVersion(Receipt receipt, Long requestVersion) {
		if (!Objects.equals(receipt.getVersion(), requestVersion)) {
			throw createVersionConflictException();
		}
	}

	// ========== 수량이 0 이상이고 DB NUMBER(19,3) 범위의 소수점 셋째 자리 이내인지 검증하는 메서드 ==========
	private void validateQuantity(BigDecimal quantity, String fieldName) {
		if (quantity == null || quantity.signum() < 0 || quantity.stripTrailingZeros().scale() > QUANTITY_SCALE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					fieldName + "은 0 이상이고 소수점 셋째 자리까지만 입력할 수 있습니다.");
		}
	}

	// ========== LOT 수량이 0보다 크고 DB NUMBER(19,3) 범위의 소수점 셋째 자리 이내인지 검증하는 메서드 ==========
	private void validatePositiveQuantity(BigDecimal quantity, String fieldName) {
		validateQuantity(quantity, fieldName);
		if (quantity.signum() == 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "은 0보다 커야 합니다.");
		}
	}

	// ========== userId로 현재 사용자를 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private AppUser findUser(Long userId) {
		return appUserRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	// ========== warehouseId로 창고를 비관적 잠금 조회하고 ACTIVE 사용 상태를 검증하는 메서드 ==========
	private Warehouse findActiveWarehouseForUpdate(Long warehouseId) {
		Warehouse warehouse = warehouseRepository.findByIdForUpdate(warehouseId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "창고를 찾을 수 없습니다."));

		if (warehouse.getStatus() != MasterStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용 중인 창고만 입고 창고로 선택할 수 있습니다.");
		}
		return warehouse;
	}

	// ========== purchaseOrderId로 발주를 비관적 잠금 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private PurchaseOrder findPurchaseOrderForUpdate(Long purchaseOrderId) {
		return purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "발주를 찾을 수 없습니다."));
	}

	// ========== receiptId로 입고를 비관적 잠금 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private Receipt findReceiptForUpdate(Long receiptId) {
		return receiptRepository.findByIdForUpdate(receiptId)
				.orElseThrow(() -> createReceiptNotFoundException());
	}

	// ========== 발주 품목과 ITEM 기본정보를 lineNo 오름차순으로 조회하는 메서드 ==========
	private List<PurchaseOrderItem> findPurchaseOrderItems(Long purchaseOrderId) {
		return purchaseOrderItemRepository.findAllByPurchaseOrderId(purchaseOrderId);
	}

	// ========== 입고 품목·원본 발주 품목·품목·LOT·재고 LOT를 발주 품목 순서로 조회하는 메서드 ==========
	private List<ReceiptItem> findReceiptItems(Long receiptId) {
		return receiptItemRepository.findAllByReceiptIdWithDetails(receiptId);
	}

	// ========== Receipt와 최신 품목·LOT·매입 전표를 상세 응답으로 변환하는 메서드 ==========
	private ReceiptDetailResponse createDetailResponse(Receipt receipt) {
		return createDetailResponse(receipt, findReceiptItems(receipt.getReceiptId()));
	}

	// ========== 이미 조회한 품목·LOT와 Receipt를 상세 응답으로 변환하여 중복 조회를 줄이는 메서드 ==========
	private ReceiptDetailResponse createDetailResponse(Receipt receipt, List<ReceiptItem> receiptItems) {
		Long voucherId = voucherRepository.findByReceiptId(receipt.getReceiptId()).map(Voucher::getVoucherId)
				.orElse(null);
		return ReceiptDetailResponse.from(receipt, receiptItems, voucherId);
	}

	// ========== 입고 INSERT·UPDATE와 자식 검수 결과를 즉시 실행하여 마지막 저장 순간의 낙관적 잠금 충돌을 확인하는 메서드 ==========
	private void flushReceiptChanges() {
		try {
			receiptRepository.flush();
		} catch (ObjectOptimisticLockingFailureException exception) {
			throw createVersionConflictException();
		}
	}

	// ========== 입고 동시 수정·검수 처리 충돌에 사용할 409 업무 예외를 생성하는 메서드 ==========
	private BusinessException createVersionConflictException() {
		return new BusinessException(ErrorCode.CONFLICT,
				"다른 사용자가 먼저 입고를 수정하거나 처리했습니다. 최신 입고 정보를 다시 조회해 주세요.");
	}

	// ========== 존재하지 않는 입고에 사용할 공통 404 업무 예외를 생성하는 메서드 ==========
	private BusinessException createReceiptNotFoundException() {
		return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "입고를 찾을 수 없습니다.");
	}

	// ========== 필수 처리 사유의 앞뒤 공백·빈 값·최대 길이를 검증하고 정규화된 값을 반환하는 메서드 ==========
	private String requireReason(String reason, String fieldName) {
		String normalizedReason = normalizeOptionalValue(reason);
		if (normalizedReason == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "를 입력해 주세요.");
		}
		if (normalizedReason.length() > 1000) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "는 1000자 이하로 입력해 주세요.");
		}
		return normalizedReason;
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

	// ========== 시작일이 종료일보다 늦지 않은 올바른 입고 등록 기간인지 검증하는 메서드 ==========
	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 늦을 수 없습니다.");
		}
	}

	// ========== 검수 저장 요청에서 정규화한 LOT 입력 값을 내부 처리 단계에 전달하기 위한 record ==========
	private record NormalizedLotInput(String supplierLotNumber, LocalDate expiryDate, BigDecimal normalQuantity) {
	}

	// ========== 검수 완료에서 확정한 잔여 처리 또는 발주 취소 내용을 발주·입고 상태 변경에 전달하기 위한 record ==========
	private record CompletionDecision(
			ReceiptRemainderAction remainderAction,
			String remainderReason,
			boolean cancelPurchaseOrder,
			String cancelReason,
			boolean supplierCancelConfirmed
	) {
		private static CompletionDecision received() {
			return new CompletionDecision(null, null, false, null, false);
		}

		private static CompletionDecision remainder(ReceiptRemainderAction action, String reason) {
			return new CompletionDecision(action, reason, false, null, false);
		}

		private static CompletionDecision cancelPurchaseOrder(String cancelReason,
				boolean supplierCancelConfirmed) {
			return new CompletionDecision(null, null, true, cancelReason, supplierCancelConfirmed);
		}
	}

	// ========== 입고 LOT와 생성·재사용할 재고 LOT를 연결하여 잠금과 재고 반영 단계에 전달하기 위한 내부 클래스 ==========
	private static final class InventoryTarget {
		private final ReceiptItem receiptItem;
		private final ReceiptLot receiptLot;
		private InventoryLot inventoryLot;

		private InventoryTarget(ReceiptItem receiptItem, ReceiptLot receiptLot, InventoryLot inventoryLot) {
			this.receiptItem = receiptItem;
			this.receiptLot = receiptLot;
			this.inventoryLot = inventoryLot;
		}

		private ReceiptItem receiptItem() {
			return receiptItem;
		}

		private ReceiptLot receiptLot() {
			return receiptLot;
		}

		private InventoryLot inventoryLot() {
			return inventoryLot;
		}

		private void replaceInventoryLot(InventoryLot inventoryLot) {
			this.inventoryLot = inventoryLot;
		}
	}
}
