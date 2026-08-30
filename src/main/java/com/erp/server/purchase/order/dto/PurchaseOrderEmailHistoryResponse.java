package com.erp.server.purchase.order.dto;

import java.time.LocalDateTime;

import com.erp.server.purchase.order.domain.PurchaseOrderEmailHistory;
import com.erp.server.purchase.order.domain.PurchaseOrderEmailStatus;

// ********** 발주서 이메일의 시도 순번·수신 주소·결과·오류·처리자·처리 일시를 반환하기 위한 응답 DTO record **********
public record PurchaseOrderEmailHistoryResponse(
		Long emailHistoryId,
		Integer attemptNo,
		String recipientEmail,
		PurchaseOrderEmailStatus status,
		String errorMessage,
		Long attemptedBy,
		String attemptedByName,
		LocalDateTime attemptedAt
) {

	// ========== PurchaseOrderEmailHistory Entity를 이메일 전송 이력 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderEmailHistoryResponse from(PurchaseOrderEmailHistory history) {
		return new PurchaseOrderEmailHistoryResponse(history.getEmailHistoryId(), history.getAttemptNo(),
				history.getRecipientEmail(), history.getStatus(), history.getErrorMessage(),
				history.getAttemptedBy() == null ? null : history.getAttemptedBy().getUserId(),
				history.getAttemptedBy() == null ? null : history.getAttemptedBy().getUserName(),
				history.getAttemptedAt());
	}
}
