package com.erp.server.master.warehouse.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 창고·품목별 안전재고를 최초 등록하거나 기존 값을 변경할 때 필요한 수량과 version을 전달받기 위한 요청 DTO record **********
public record WarehouseItemUpdateRequest(

		@NotNull
		@DecimalMin(value = "0.000")
		@Digits(integer = 16, fraction = 3)
		BigDecimal safetyStockQuantity,

		@PositiveOrZero
		Long version // 최초 등록 시 생략하고 기존 안전재고 변경 시 조회한 최신 version을 전달한다.
) {
}
