package com.erp.server.purchase.order.dto;

import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;
import com.erp.server.purchase.order.domain.PurchaseOrderStatus;

// ********** 발주 확정 후 자동 전송 또는 재전송 결과와 저장된 이메일 이력을 반환하기 위한 응답 DTO record **********
public record PurchaseOrderEmailSendResponse(
		Long purchaseOrderId,
		PurchaseOrderStatus purchaseOrderStatus,
		PurchaseOrderEmailStatus emailStatus,
		PurchaseOrderEmailHistoryResponse history,
		Long version
) {
}
