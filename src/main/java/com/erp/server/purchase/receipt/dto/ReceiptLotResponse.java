package com.erp.server.purchase.receipt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erp.server.purchase.receipt.domain.ReceiptLot;

// ********** 입고 LOT의 공급업체 번호·적용 번호·사용기한·정상 수량·재고 LOT 연결을 반환하기 위한 응답 DTO record **********
public record ReceiptLotResponse(
		Long receiptLotId,
		String supplierLotNumber,
		String lotNumber,
		boolean internalLot,
		LocalDate expiryDate,
		BigDecimal normalQuantity,
		Long inventoryLotId
) {

	// ========== ReceiptLot Entity를 화면과 후속 업무에서 사용할 입고 LOT 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static ReceiptLotResponse from(ReceiptLot receiptLot) {
		return new ReceiptLotResponse(receiptLot.getReceiptLotId(), receiptLot.getSupplierLotNumber(),
				receiptLot.getLotNumber(), receiptLot.isInternalLot(), receiptLot.getExpiryDate(),
				receiptLot.getNormalQuantity(), receiptLot.getInventoryLot() == null ? null
						: receiptLot.getInventoryLot().getInventoryLotId());
	}
}
