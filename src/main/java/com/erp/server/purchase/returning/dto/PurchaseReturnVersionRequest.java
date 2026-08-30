package com.erp.server.purchase.returning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 매입 반품 완료처럼 추가 입력 없이 최신 상태만 검증하는 요청 DTO record **********
public record PurchaseReturnVersionRequest(
		@NotNull(message = "매입 반품 version이 필요합니다.")
		@PositiveOrZero(message = "매입 반품 version이 올바르지 않습니다.") Long version
) {
}
