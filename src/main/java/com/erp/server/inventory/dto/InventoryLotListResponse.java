package com.erp.server.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.server.inventory.domain.InventoryLotStatus;

// ********** 재고 LOT 목록과 주문 작성 화면의 창고별 가용재고 계산에 필요한 값을 반환하기 위한 응답 DTO record **********
public record InventoryLotListResponse(Long inventoryLotId, Long warehouseId, String warehouseCode,
		String warehouseName, Long itemId, String itemCode, String itemName, Long supplierId, String supplierCode,
		String supplierName, String lotNumber, String supplierLotNumber, boolean internalLot, LocalDate expiryDate,
		InventoryLotStatus status, BigDecimal currentQuantity, BigDecimal reservedQuantity,
		BigDecimal availableQuantity, boolean expired, boolean inventoryWorkRestricted,
		boolean outboundAvailable, BigDecimal outboundAvailableQuantity, Long version) {
}
