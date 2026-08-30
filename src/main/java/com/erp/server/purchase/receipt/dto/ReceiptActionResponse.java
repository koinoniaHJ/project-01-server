package com.erp.server.purchase.receipt.dto;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;

// ********** 입고 등록·검수 시작·완료·취소 처리자와 처리 일시를 반환하기 위한 응답 DTO record **********
public record ReceiptActionResponse(Long userId, String userName, LocalDateTime processedAt) {

	// ========== 처리 사용자와 일시가 존재하면 입고 처리 이력 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static ReceiptActionResponse from(AppUser user, LocalDateTime processedAt) {
		if (user == null && processedAt == null) {
			return null;
		}

		return new ReceiptActionResponse(user == null ? null : user.getUserId(),
				user == null ? null : user.getUserName(), processedAt);
	}
}
