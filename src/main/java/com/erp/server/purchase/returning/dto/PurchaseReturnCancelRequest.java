package com.erp.server.purchase.returning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** REGISTERED 매입 반품을 취소할 사유와 최신 version을 전달하기 위한 요청 DTO record **********
public record PurchaseReturnCancelRequest(
		@NotBlank(message = "매입 반품 취소 사유를 입력해 주세요.")
		@Size(max = 1000, message = "매입 반품 취소 사유는 1000자 이하로 입력해 주세요.") String reason,
		@NotNull(message = "매입 반품 version이 필요합니다.")
		@PositiveOrZero(message = "매입 반품 version이 올바르지 않습니다.") Long version
) {
}
