package com.erp.server.purchase.order.dto;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;

// ********** 발주의 승인 요청·승인·확정·취소·종료·공급업체 취소 확인 처리자와 처리 일시를 반환하기 위한 응답 DTO record **********
public record PurchaseOrderActionResponse(Long userId, String userName, LocalDateTime processedAt) {

	// ========== 처리 사용자와 일시가 존재하면 발주 처리 이력 응답으로 변환하는 정적 팩토리 메서드 ==========
	public static PurchaseOrderActionResponse from(AppUser user, LocalDateTime processedAt) {
		if (user == null && processedAt == null) {
			return null;
		}

		return new PurchaseOrderActionResponse(user == null ? null : user.getUserId(),
				user == null ? null : user.getUserName(), processedAt);
	}
}
