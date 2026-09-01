package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.server.inventory.domain.InventoryLot;

// ********** 선택 창고·주문 품목에서 포장에 사용할 수 있는 재고 LOT와 최신 가용 수량을 반환하기 위한 응답 DTO record **********
public record ShipmentAvailableLotResponse(
		Long salesOrderItemId, Long itemId, String itemCode, String itemName, Long inventoryLotId, String lotNumber,
		String supplierLotNumber, Long supplierId, String supplierCode, String supplierName, LocalDate expiryDate,
		BigDecimal currentQuantity, BigDecimal reservedQuantity, BigDecimal availableQuantity
) {
	public static ShipmentAvailableLotResponse from(Long salesOrderItemId, InventoryLot inventoryLot) {
		return new ShipmentAvailableLotResponse(salesOrderItemId, inventoryLot.getItem().getItemId(),
				inventoryLot.getItem().getItemCode(), inventoryLot.getItem().getItemName(),
				inventoryLot.getInventoryLotId(), inventoryLot.getLotNumber(), inventoryLot.getSupplierLotNumber(),
				inventoryLot.getSupplier().getSupplierId(), inventoryLot.getSupplier().getSupplierCode(),
				inventoryLot.getSupplier().getSupplierName(), inventoryLot.getExpiryDate(),
				inventoryLot.getCurrentQuantity(), inventoryLot.getReservedQuantity(),
				inventoryLot.calculateAvailableQuantity());
	}
}
