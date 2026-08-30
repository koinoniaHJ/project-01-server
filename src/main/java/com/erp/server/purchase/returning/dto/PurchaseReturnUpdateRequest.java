package com.erp.server.purchase.returning.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** REGISTERED 매입 반품의 LOT별 수량·사유와 최신 version을 전체 교체하기 위한 요청 DTO record **********
public record PurchaseReturnUpdateRequest(
		@NotEmpty(message = "반품 품목을 하나 이상 선택해 주세요.") List<@Valid PurchaseReturnItemRequest> items,
		@NotBlank(message = "매입 반품 사유를 입력해 주세요.")
		@Size(max = 1000, message = "매입 반품 사유는 1000자 이하로 입력해 주세요.") String reason,
		@NotNull(message = "매입 반품 version이 필요합니다.")
		@PositiveOrZero(message = "매입 반품 version이 올바르지 않습니다.") Long version
) {
}
