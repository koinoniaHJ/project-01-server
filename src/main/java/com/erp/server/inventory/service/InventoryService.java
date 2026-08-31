package com.erp.server.inventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.user.domain.AppUser;
import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.inventory.domain.StockMovement;
import com.erp.server.inventory.domain.StockMovementType;
import com.erp.server.inventory.domain.InventoryLotStatus;
import com.erp.server.inventory.dto.InventoryLotListResponse;
import com.erp.server.inventory.repository.InventoryLotRepository;
import com.erp.server.inventory.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

// ********** 입고·반품·출고·조정·폐기 업무가 공통으로 사용하는 LOT 잠금·재고/예약 수량 검증·변동 이력 저장을 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

	private static final int QUANTITY_SCALE = 3;

	private final InventoryLotRepository inventoryLotRepository;
	private final StockMovementRepository stockMovementRepository;

	// ========== 창고·품목·상태·사용기한 조건으로 LOT별 현재·예약·가용 수량을 조회하는 메서드 ==========
	// 주문 작성 화면은 itemId로 조회한 출고 가능 수량을 창고별로 합산하며 이 조회만으로 재고를 예약하지 않는다.
	public List<InventoryLotListResponse> getInventoryLots(Long warehouseId, Long itemId,
			InventoryLotStatus status, LocalDate expiry) {
		return inventoryLotRepository.findAllByFilters(warehouseId, itemId, status, expiry).stream()
				.map(this::createInventoryLotListResponse).toList();
	}

	// ========== LOT 상태·사용기한·실사/조정 제한을 반영하여 조회 응답의 실제 출고 가능 수량을 계산하는 메서드 ==========
	private InventoryLotListResponse createInventoryLotListResponse(InventoryLot inventoryLot) {
		boolean expired = inventoryLot.isExpired(LocalDate.now());
		boolean restricted = inventoryLotRepository.countUnreleasedRestrictions(inventoryLot.getInventoryLotId()) > 0;
		BigDecimal availableQuantity = inventoryLot.calculateAvailableQuantity();
		boolean outboundAvailable = inventoryLot.isAvailableStatus() && !expired && !restricted
				&& availableQuantity.compareTo(BigDecimal.ZERO) > 0;

		return new InventoryLotListResponse(inventoryLot.getInventoryLotId(),
				inventoryLot.getWarehouse().getWarehouseId(), inventoryLot.getWarehouse().getWarehouseCode(),
				inventoryLot.getWarehouse().getWarehouseName(), inventoryLot.getItem().getItemId(),
				inventoryLot.getItem().getItemCode(), inventoryLot.getItem().getItemName(),
				inventoryLot.getSupplier().getSupplierId(), inventoryLot.getSupplier().getSupplierCode(),
				inventoryLot.getSupplier().getSupplierName(), inventoryLot.getLotNumber(),
				inventoryLot.getSupplierLotNumber(), inventoryLot.isInternalLot(), inventoryLot.getExpiryDate(),
				inventoryLot.getStatus(), inventoryLot.getCurrentQuantity(), inventoryLot.getReservedQuantity(),
				availableQuantity, expired, restricted, outboundAvailable,
				outboundAvailable ? availableQuantity : BigDecimal.ZERO, inventoryLot.getVersion());
	}

	// ========== inventoryLotId로 재고 LOT와 기본정보를 일반 조회하는 메서드 ==========
	public InventoryLot getInventoryLot(Long inventoryLotId) {
		return inventoryLotRepository.findByIdWithDetails(inventoryLotId)
				.orElseThrow(() -> createInventoryLotNotFoundException());
	}

	// ========== 동일 창고·품목·공급업체·LOT 번호의 기존 재고 LOT를 일반 조회하는 메서드 ==========
	// 입고 업무에서 반환값이 null이면 신규 LOT를 생성하고, 기존 LOT이면 입력 사용기한과의 일치 여부를 추가 검증한다.
	public InventoryLot findInventoryLot(Long warehouseId, Long itemId, Long supplierId, String lotNumber) {
		return inventoryLotRepository.findByBusinessKey(warehouseId, itemId, supplierId, lotNumber).orElse(null);
	}

	// ========== 공급업체 LOT가 없을 때 사용할 LOT000001 형식의 다음 내부 LOT 번호를 생성하는 메서드 ==========
	public String generateInternalLotNumber() {
		return inventoryLotRepository.generateInternalLotNumber();
	}

	// ========== 단일 재고 LOT의 수량·상태 변경을 위해 PESSIMISTIC_WRITE 비관적 잠금으로 조회하는 메서드 ==========
	@Transactional
	public InventoryLot getInventoryLotForUpdate(Long inventoryLotId) {
		return findInventoryLotForUpdate(inventoryLotId);
	}

	// ========== 여러 재고 LOT를 중복 제거 후 식별자 오름차순의 고정 순서로 PESSIMISTIC_WRITE 잠금 조회하는 메서드 ==========
	// 반환된 잠금은 이 메서드를 호출한 상위 업무 트랜잭션이 끝날 때까지 유지된다.
	@Transactional
	public List<InventoryLot> getInventoryLotsForUpdate(Collection<Long> inventoryLotIds) {
		if (inventoryLotIds == null || inventoryLotIds.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "잠금 조회할 재고 LOT를 하나 이상 선택해 주세요.");
		}

		List<Long> orderedIds = inventoryLotIds.stream()
				.peek(this::validateIdentifier)
				.collect(java.util.stream.Collectors.collectingAndThen(
						java.util.stream.Collectors.toCollection(LinkedHashSet::new),
						ids -> ids.stream().sorted().toList()));
		List<InventoryLot> inventoryLots = inventoryLotRepository.findAllByIdForUpdate(orderedIds);

		if (inventoryLots.size() != orderedIds.size()) {
			throw createInventoryLotNotFoundException();
		}

		return inventoryLots;
	}

	// ========== 현재 재고에서 예약 수량을 차감하여 LOT의 가용 수량을 계산하는 메서드 ==========
	// 출고 가능 여부를 판단할 때는 계산 전 validateLotForOutbound 메서드로 상태·사용기한·제한을 함께 검증한다.
	public BigDecimal calculateAvailableQuantity(InventoryLot inventoryLot) {
		return inventoryLot.calculateAvailableQuantity();
	}

	// ========== LOT가 출고 가능 상태이고 사용기한이 지나지 않았으며 실사/조정 제한이 없는지 검증하는 메서드 ==========
	public void validateLotForOutbound(InventoryLot inventoryLot) {
		if (!inventoryLot.isAvailableStatus()) {
			throw new BusinessException(ErrorCode.CONFLICT, "출고 제한 상태의 재고 LOT는 출고할 수 없습니다.");
		}

		if (inventoryLot.isExpired(LocalDate.now())) {
			throw new BusinessException(ErrorCode.CONFLICT, "사용기한이 지난 재고 LOT는 출고할 수 없습니다.");
		}

		validateNoInventoryWorkRestriction(inventoryLot);
	}

	// ========== 입고 검수 완료 수량을 현재 재고에 증가시키고 RECEIPT 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	@Transactional
	public InventoryLot increaseFromReceipt(Long inventoryLotId, BigDecimal quantity, Long receiptLotId,
			AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateCommonMovementInput(quantity, receiptLotId, processedBy);
		validateNoInventoryWorkRestriction(inventoryLot);

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.increaseCurrentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createReceipt(
				inventoryLot, quantity, beforeQuantity, receiptLotId, processedBy));

		return inventoryLot;
	}

	// ========== 거래처 반품의 재판매 가능 수량을 현재 재고에 증가시키고 RETURN_IN 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	@Transactional
	public InventoryLot increaseFromCustomerReturn(Long inventoryLotId, BigDecimal quantity,
			Long customerReturnItemId, AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateCommonMovementInput(quantity, customerReturnItemId, processedBy);
		validateNoInventoryWorkRestriction(inventoryLot);

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.increaseCurrentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createReturnIn(
				inventoryLot, quantity, beforeQuantity, customerReturnItemId, processedBy));

		return inventoryLot;
	}

	// ========== 승인된 증가 재고 조정을 현재 재고에 반영하고 ADJUSTMENT_IN 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	// 해당 재고 조정 자체가 실사 제한을 해소하는 후속 업무이므로 일반 제한 검증은 적용하지 않는다.
	@Transactional
	public InventoryLot increaseFromAdjustment(Long inventoryLotId, BigDecimal quantity, Long stockAdjustmentId,
			String reason, AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateCommonMovementInput(quantity, stockAdjustmentId, processedBy);

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.increaseCurrentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createAdjustment(inventoryLot, StockMovementType.ADJUSTMENT_IN,
				quantity, beforeQuantity, stockAdjustmentId, normalizeReason(reason), processedBy));

		return inventoryLot;
	}

	// ========== 매입 반품 완료 수량을 현재 재고에서 감소시키고 PURCHASE_RETURN 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	@Transactional
	public InventoryLot decreaseForPurchaseReturn(Long inventoryLotId, BigDecimal quantity,
			Long purchaseReturnItemId, AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateCommonMovementInput(quantity, purchaseReturnItemId, processedBy);
		validateNoInventoryWorkRestriction(inventoryLot);
		validateAvailableQuantity(inventoryLot, quantity);

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.decreaseCurrentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createPurchaseReturn(
				inventoryLot, quantity, beforeQuantity, purchaseReturnItemId, processedBy));

		return inventoryLot;
	}

	// ========== 승인된 감소 재고 조정을 현재 재고에 반영하고 ADJUSTMENT_OUT 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	// 해당 재고 조정 자체가 실사 제한을 해소하는 후속 업무이므로 일반 제한 검증은 적용하지 않는다.
	@Transactional
	public InventoryLot decreaseForAdjustment(Long inventoryLotId, BigDecimal quantity, Long stockAdjustmentId,
			String reason, AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateCommonMovementInput(quantity, stockAdjustmentId, processedBy);
		validateAvailableQuantity(inventoryLot, quantity);

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.decreaseCurrentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createAdjustment(inventoryLot, StockMovementType.ADJUSTMENT_OUT,
				quantity, beforeQuantity, stockAdjustmentId, normalizeReason(reason), processedBy));

		return inventoryLot;
	}

	// ========== 포장 완료된 출고 수량을 LOT 예약 수량에 증가시키는 메서드 ==========
	// 예약은 물리 재고 증감이 아니므로 STOCK_MOVEMENT 이력을 생성하지 않는다.
	@Transactional
	public InventoryLot reserveForShipment(Long inventoryLotId, BigDecimal quantity) {
		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateQuantity(quantity);
		validateLotForOutbound(inventoryLot);
		validateAvailableQuantity(inventoryLot, quantity);

		inventoryLot.increaseReservedQuantity(quantity);
		return inventoryLot;
	}

	// ========== 포장 취소된 출고 수량을 LOT 예약 수량에서 감소시키는 메서드 ==========
	// 이미 반영된 예약을 되돌리는 처리는 LOT 상태·사용기한·실사 제한과 관계없이 허용하며 STOCK_MOVEMENT를 생성하지 않는다.
	@Transactional
	public InventoryLot releaseShipmentReservation(Long inventoryLotId, BigDecimal quantity) {
		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateQuantity(quantity);

		if (inventoryLot.getReservedQuantity().compareTo(quantity) < 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "해제할 수량이 현재 예약 수량을 초과합니다.");
		}

		inventoryLot.decreaseReservedQuantity(quantity);
		return inventoryLot;
	}

	// ========== 출고 완료 수량을 현재·예약 수량에서 함께 감소시키고 SHIPMENT 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	@Transactional
	public InventoryLot completeShipment(Long inventoryLotId, BigDecimal quantity, Long shipmentLotId,
			AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateCommonMovementInput(quantity, shipmentLotId, processedBy);
		validateLotForOutbound(inventoryLot);

		if (inventoryLot.getReservedQuantity().compareTo(quantity) < 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "출고 완료 수량이 현재 예약 수량을 초과합니다.");
		}

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.completeShipmentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createShipment(
				inventoryLot, quantity, beforeQuantity, shipmentLotId, processedBy));

		return inventoryLot;
	}

	// ========== 사용기한 경과 또는 BLOCKED LOT의 가용 수량을 감소시키고 DISPOSAL 변동 이력을 같은 트랜잭션으로 저장하는 메서드 ==========
	@Transactional
	public InventoryLot disposeInventory(Long inventoryLotId, BigDecimal quantity, String reason,
			AppUser processedBy) {

		InventoryLot inventoryLot = findInventoryLotForUpdate(inventoryLotId);
		validateQuantity(quantity);
		validateProcessedBy(processedBy);
		validateNoInventoryWorkRestriction(inventoryLot);
		validateDisposalTarget(inventoryLot);
		validateAvailableQuantity(inventoryLot, quantity);
		String normalizedReason = requireReason(reason);

		BigDecimal beforeQuantity = inventoryLot.getCurrentQuantity();
		inventoryLot.decreaseCurrentQuantity(quantity);
		stockMovementRepository.save(StockMovement.createDisposal(
				inventoryLot, quantity, beforeQuantity, normalizedReason, processedBy));

		return inventoryLot;
	}

	// ========== inventoryLotId로 재고 LOT를 비관적 잠금 조회하고 없으면 404 예외를 발생시키는 메서드 ==========
	private InventoryLot findInventoryLotForUpdate(Long inventoryLotId) {
		validateIdentifier(inventoryLotId);
		return inventoryLotRepository.findByIdForUpdate(inventoryLotId)
				.orElseThrow(() -> createInventoryLotNotFoundException());
	}

	// ========== 재고 LOT에 해제되지 않은 실사 또는 후속 재고 조정 제한이 없는지 검증하는 메서드 ==========
	private void validateNoInventoryWorkRestriction(InventoryLot inventoryLot) {
		if (inventoryLotRepository.countUnreleasedRestrictions(inventoryLot.getInventoryLotId()) > 0) {
			throw new BusinessException(ErrorCode.RESOURCE_LOCKED,
					"재고 실사 또는 재고 조정이 진행 중인 LOT는 현재 업무에서 처리할 수 없습니다.");
		}
	}

	// ========== 감소 또는 예약 요청 수량이 현재 재고에서 예약 수량을 차감한 가용 수량을 초과하지 않는지 검증하는 메서드 ==========
	private void validateAvailableQuantity(InventoryLot inventoryLot, BigDecimal quantity) {
		if (inventoryLot.calculateAvailableQuantity().compareTo(quantity) < 0) {
			throw new BusinessException(ErrorCode.CONFLICT, "처리 수량이 재고 LOT의 최신 가용 수량을 초과합니다.");
		}
	}

	// ========== 폐기 대상 LOT가 사용기한 경과 또는 BLOCKED 상태인지 검증하는 메서드 ==========
	private void validateDisposalTarget(InventoryLot inventoryLot) {
		if (!inventoryLot.isExpired(LocalDate.now()) && inventoryLot.isAvailableStatus()) {
			throw new BusinessException(ErrorCode.CONFLICT,
					"사용기한이 지났거나 출고 제한 상태인 재고 LOT만 폐기할 수 있습니다.");
		}
	}

	// ========== 재고 변동에 공통으로 필요한 수량·원본 업무 식별자·처리 사용자를 검증하는 메서드 ==========
	private void validateCommonMovementInput(BigDecimal quantity, Long originId, AppUser processedBy) {
		validateQuantity(quantity);
		validateIdentifier(originId);
		validateProcessedBy(processedBy);
	}

	// ========== 재고와 예약 수량에 적용할 값이 0보다 크고 소수점 셋째 자리 이내인지 검증하는 메서드 ==========
	private void validateQuantity(BigDecimal quantity) {
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "처리 수량은 0보다 커야 합니다.");
		}

		if (quantity.stripTrailingZeros().scale() > QUANTITY_SCALE) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "처리 수량은 소수점 셋째 자리까지만 입력할 수 있습니다.");
		}
	}

	// ========== 재고 LOT 또는 원본 업무 식별자가 양수인지 검증하는 메서드 ==========
	private void validateIdentifier(Long identifier) {
		if (identifier == null || identifier <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "대상 식별자가 올바르지 않습니다.");
		}
	}

	// ========== 재고 변동 이력에 저장할 처리 사용자가 존재하는지 검증하는 메서드 ==========
	private void validateProcessedBy(AppUser processedBy) {
		if (processedBy == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "재고 처리 사용자를 확인할 수 없습니다.");
		}
	}

	// ========== 선택 입력인 재고 변동 사유의 앞뒤 공백을 제거하고 빈 값은 null로 변환하는 메서드 ==========
	private String normalizeReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}

		String normalizedReason = reason.trim();
		if (normalizedReason.length() > 1000) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "재고 변동 사유는 1000자 이하로 입력해 주세요.");
		}

		return normalizedReason;
	}

	// ========== 폐기 이력에 필요한 사유가 존재하는지 확인하고 정규화된 값을 반환하는 메서드 ==========
	private String requireReason(String reason) {
		String normalizedReason = normalizeReason(reason);

		if (normalizedReason == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "재고 폐기 사유를 입력해 주세요.");
		}

		return normalizedReason;
	}

	// ========== 존재하지 않는 재고 LOT에 사용할 공통 404 업무 예외를 생성하는 메서드 ==========
	private BusinessException createInventoryLotNotFoundException() {
		return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "재고 LOT를 찾을 수 없습니다.");
	}
}
