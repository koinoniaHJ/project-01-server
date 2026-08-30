package com.erp.server.purchase.returning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.server.master.item.domain.ItemUnit;

// ********** 매입 반품 상세의 원본 LOT·재고·가능 수량·등록 수량·금액을 역할별로 반환하기 위한 응답 DTO record **********
public record PurchaseReturnItemResponse(
		Long purchaseReturnItemId,
		Long receiptLotId,
		Long inventoryLotId,
		Integer lineNo,
		Long itemId,
		String itemCode,
		String itemName,
		ItemUnit unit,
		String otherUnitName,
		String supplierLotNumber,
		String lotNumber,
		LocalDate expiryDate,
		BigDecimal receivedQuantity,
		BigDecimal completedReturnQuantity,
		BigDecimal currentQuantity,
		BigDecimal reservedQuantity,
		BigDecimal availableQuantity,
		BigDecimal returnableQuantity,
		BigDecimal returnQuantity,
		BigDecimal unitPrice,
		BigDecimal lineAmount
) {
}
