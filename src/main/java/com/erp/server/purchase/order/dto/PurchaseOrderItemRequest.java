package com.erp.server.purchase.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// ********** 발주에 포함할 품목 식별자·발주 수량·작성 시점 매입 단가를 전달받기 위한 요청 DTO record **********
public record PurchaseOrderItemRequest(

		@NotNull(message = "품목 식별자는 필수입니다.")
		@Positive(message = "품목 식별자는 1 이상이어야 합니다.")
		Long itemId,

		@NotNull(message = "발주 수량은 필수입니다.")
		@DecimalMin(value = "0.001", message = "발주 수량은 0보다 커야 합니다.")
		@Digits(integer = 16, fraction = 3, message = "발주 수량은 정수 16자리와 소수 3자리 이하여야 합니다.")
		BigDecimal orderedQuantity,

		@NotNull(message = "발주 단가는 필수입니다.")
		@DecimalMin(value = "0.00", message = "발주 단가는 0 이상이어야 합니다.")
		@Digits(integer = 17, fraction = 2, message = "발주 단가는 정수 17자리와 소수 2자리 이하여야 합니다.")
		BigDecimal unitPrice
) {
}
