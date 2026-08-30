package com.erp.server.purchase.receipt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// ********** 검수 대기 입고 등록에 필요한 원본 발주와 입고 창고 식별자를 전달하기 위한 요청 DTO record **********
public record ReceiptCreateRequest(
		@NotNull(message = "발주를 선택해 주세요.") @Positive(message = "발주 식별자가 올바르지 않습니다.") Long purchaseOrderId,
		@NotNull(message = "입고 창고를 선택해 주세요.") @Positive(message = "창고 식별자가 올바르지 않습니다.") Long warehouseId
) {
}
