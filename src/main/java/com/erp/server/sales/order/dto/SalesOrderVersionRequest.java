package com.erp.server.sales.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 주문 삭제·접수 시 조회한 최신 version을 전달하기 위한 요청 DTO record **********
public record SalesOrderVersionRequest(
		@NotNull(message = "주문 version은 필수입니다.")
		@PositiveOrZero(message = "주문 version은 0 이상이어야 합니다.") Long version
) {
}
