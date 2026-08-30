package com.erp.server.purchase.returning.dto;

import com.erp.server.purchase.returning.domain.PurchaseReturnStatus;

// ********** 매입 반품 완료 후 반품 상태·생성 전표·최신 version을 반환하기 위한 응답 DTO record **********
public record PurchaseReturnCompleteResponse(
		Long purchaseReturnId,
		PurchaseReturnStatus status,
		Long purchaseReturnVoucherId,
		Long version
) {
}
