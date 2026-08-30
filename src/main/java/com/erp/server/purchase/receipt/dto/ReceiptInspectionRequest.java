package com.erp.server.purchase.receipt.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 검수 중 입고의 모든 품목 결과와 최신 version을 한 번에 전달하기 위한 요청 DTO record **********
public record ReceiptInspectionRequest(
		@NotEmpty(message = "검수 품목은 하나 이상이어야 합니다.") List<@Valid ReceiptInspectionItemRequest> items,
		@NotNull(message = "입고 version이 필요합니다.") @PositiveOrZero(message = "입고 version이 올바르지 않습니다.") Long version
) {
}
