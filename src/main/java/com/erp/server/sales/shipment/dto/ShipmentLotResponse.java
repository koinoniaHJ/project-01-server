package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.server.inventory.domain.InventoryLot;
import com.erp.server.inventory.domain.InventoryLotStatus;
import com.erp.server.sales.shipment.domain.ShipmentLot;

// ********** 출고 품목에 배정된 재고 LOT·공급업체·수량·예약 상태를 반환하기 위한 응답 DTO record **********
public record ShipmentLotResponse(
		Long shipmentLotId, Long inventoryLotId, String lotNumber, String supplierLotNumber, Long supplierId,
		String supplierCode, String supplierName, LocalDate expiryDate, InventoryLotStatus status,
		BigDecimal currentQuantity, BigDecimal reservedQuantity, BigDecimal availableQuantity,
		boolean inventoryWorkRestricted, BigDecimal packedQuantity, boolean reserved
) {
	// ========== ShipmentLot Entity와 실사/조정 제한 여부를 최신 재고 수량이 포함된 포장 LOT 응답으로 변환하는 메서드 ==========
	public static ShipmentLotResponse from(ShipmentLot shipmentLot, boolean inventoryWorkRestricted) {
		InventoryLot inventoryLot = shipmentLot.getInventoryLot();
		return new ShipmentLotResponse(shipmentLot.getShipmentLotId(),
				inventoryLot.getInventoryLotId(), inventoryLot.getLotNumber(), inventoryLot.getSupplierLotNumber(),
				inventoryLot.getSupplier().getSupplierId(), inventoryLot.getSupplier().getSupplierCode(),
				inventoryLot.getSupplier().getSupplierName(), inventoryLot.getExpiryDate(), inventoryLot.getStatus(),
				inventoryLot.getCurrentQuantity(), inventoryLot.getReservedQuantity(),
				inventoryLot.calculateAvailableQuantity(), inventoryWorkRestricted, shipmentLot.getPackedQuantity(),
				shipmentLot.isReserved());
	}
}
