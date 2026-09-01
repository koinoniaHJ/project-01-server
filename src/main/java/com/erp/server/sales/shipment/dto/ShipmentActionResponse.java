package com.erp.server.sales.shipment.dto;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;

// ********** 출고 포장·완료·취소 처리자와 처리 일시를 반환하기 위한 응답 DTO record **********
public record ShipmentActionResponse(Long userId, String userName, LocalDateTime processedAt) {
	public static ShipmentActionResponse from(AppUser user, LocalDateTime processedAt) {
		if (user == null && processedAt == null) return null;
		return new ShipmentActionResponse(user == null ? null : user.getUserId(),
				user == null ? null : user.getUserName(), processedAt);
	}
}
