package com.erp.server.sales.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** REGISTERED 주문의 취소 사유와 최신 version을 전달하기 위한 요청 DTO record **********
public record SalesOrderCancelRequest(
		@NotBlank(message = "주문 취소 사유를 입력해 주세요.")
		@Size(max = 1000, message = "주문 취소 사유는 1000자 이하로 입력해 주세요.") String reason,
		@NotNull(message = "주문 version은 필수입니다.")
		@PositiveOrZero(message = "주문 version은 0 이상이어야 합니다.") Long version
) {
}
