package com.erp.server.purchase.returning.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// ********** 원본 입고 LOT별 매입 반품 수량을 등록·수정 요청으로 전달하기 위한 DTO record **********
public record PurchaseReturnItemRequest(
		@NotNull(message = "원본 입고 LOT 식별자가 필요합니다.")
		@Positive(message = "원본 입고 LOT 식별자가 올바르지 않습니다.") Long receiptLotId,
		@NotNull(message = "반품 수량을 입력해 주세요.")
		@Positive(message = "반품 수량은 0보다 커야 합니다.")
		@Digits(integer = 16, fraction = 3, message = "반품 수량은 소수점 셋째 자리까지 입력할 수 있습니다.") BigDecimal returnQuantity
) {
}
