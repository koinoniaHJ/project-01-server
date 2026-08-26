package com.erp.server.master.customer.dto;

import java.time.LocalDateTime;

import com.erp.server.master.customer.domain.CustomerTradeStatus;
import com.erp.server.master.customer.domain.CustomerTradeStatusHistory;

// ********** 거래처 거래 상태의 변경 전·후 상태, 사유, 처리자와 처리 일시를 반환하기 위한 응답 DTO record **********
// 거래 상태 변경 이력 조회 API가 변경 전·후 상태, 사유, 처리자와 처리 일시를 반환하기 위한 DTO
// customerId는 API 경로에 이미 포함되므로 응답마다 반복하지 않는다.
public record CustomerTradeStatusHistoryResponse(Long tradeStatusHistoryId, CustomerTradeStatus previousStatus,
		CustomerTradeStatus changedStatus, String reason, Long changedByUserId, String changedByUserName,
		LocalDateTime changedAt) {

	// ========== CustomerTradeStatusHistory Entity를 거래 상태 변경 이력 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static CustomerTradeStatusHistoryResponse from(CustomerTradeStatusHistory history) {

		return new CustomerTradeStatusHistoryResponse(history.getTradeStatusHistoryId(), history.getPreviousStatus(),
				history.getChangedStatus(), history.getReason(), history.getChangedBy().getUserId(),
				history.getChangedBy().getUserName(), history.getChangedAt());
	}
}