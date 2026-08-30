package com.erp.server.purchase.receipt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 검수 시작처럼 추가 입력 없이 입고의 최신 version만 검증하는 요청 DTO record **********
public record ReceiptVersionRequest(
		@NotNull(message = "입고 version이 필요합니다.") @PositiveOrZero(message = "입고 version이 올바르지 않습니다.") Long version
) {
}
