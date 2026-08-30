package com.erp.server.purchase.returning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.server.master.item.domain.ItemUnit;

// ********** 완료 입고 LOT별 원본 수량·재고·완료 반품 누계·반품 가능 수량·매입 단가를 등록 화면에 반환하기 위한 응답 DTO record **********
public record PurchaseReturnSourceItemResponse(
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
		BigDecimal unitPrice
) {
}
