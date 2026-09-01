package com.erp.server.sales.shipment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// ********** 주문 품목에 배정할 재고 LOT와 포장 수량을 전달하기 위한 요청 DTO record **********
public record ShipmentLotAllocationRequest(
		@NotNull(message = "주문 품목 식별자가 필요합니다.") @Positive(message = "주문 품목 식별자가 올바르지 않습니다.") Long salesOrderItemId,
		@NotNull(message = "재고 LOT 식별자가 필요합니다.") @Positive(message = "재고 LOT 식별자가 올바르지 않습니다.") Long inventoryLotId,
		@NotNull(message = "포장 수량을 입력해 주세요.")
		@DecimalMin(value = "0.000", inclusive = false, message = "포장 수량은 0보다 커야 합니다.")
		@Digits(integer = 16, fraction = 3, message = "포장 수량은 소수점 셋째 자리까지 입력할 수 있습니다.") BigDecimal packedQuantity
) {
}
