package com.erp.server.purchase.returning.dto;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;

// ********** 매입 반품 등록·완료·취소 처리자와 처리 일시를 반환하기 위한 응답 DTO record **********
public record PurchaseReturnActionResponse(Long userId, String userName, LocalDateTime processedAt) {

	// ========== 처리 사용자와 일시가 존재하면 매입 반품 처리 이력 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseReturnActionResponse from(AppUser user, LocalDateTime processedAt) {
		if (user == null && processedAt == null) {
			return null;
		}

		return new PurchaseReturnActionResponse(user == null ? null : user.getUserId(),
				user == null ? null : user.getUserName(), processedAt);
	}
}
