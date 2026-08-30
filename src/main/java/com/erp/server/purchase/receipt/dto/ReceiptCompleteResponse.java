package com.erp.server.purchase.receipt.dto;

import com.erp.server.purchase.order.domain.PurchaseOrderStatus;
import com.erp.server.purchase.receipt.domain.ReceiptStatus;

// ********** 입고 검수 완료 후 입고·발주 상태와 생성된 매입 전표 식별자를 반환하기 위한 응답 DTO record **********
public record ReceiptCompleteResponse(
		Long receiptId,
		ReceiptStatus receiptStatus,
		Long purchaseOrderId,
		PurchaseOrderStatus purchaseOrderStatus,
		Long purchaseVoucherId,
		Long version
) {
}
