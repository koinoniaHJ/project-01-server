package com.erp.server.purchase.receipt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 검수 대기 입고의 창고 변경과 낙관적 잠금 검증 값을 전달하기 위한 요청 DTO record **********
public record ReceiptWarehouseUpdateRequest(
		@NotNull(message = "입고 창고를 선택해 주세요.") @Positive(message = "창고 식별자가 올바르지 않습니다.") Long warehouseId,
		@NotNull(message = "입고 version이 필요합니다.") @PositiveOrZero(message = "입고 version이 올바르지 않습니다.") Long version
) {
}
