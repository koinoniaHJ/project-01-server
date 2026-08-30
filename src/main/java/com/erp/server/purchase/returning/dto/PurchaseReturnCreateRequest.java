package com.erp.server.purchase.returning.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// ********** 완료 입고·원본 LOT별 수량·반품 사유로 REGISTERED 매입 반품을 등록하기 위한 요청 DTO record **********
public record PurchaseReturnCreateRequest(
		@NotNull(message = "원본 입고 식별자가 필요합니다.")
		@Positive(message = "원본 입고 식별자가 올바르지 않습니다.") Long receiptId,
		@NotEmpty(message = "반품 품목을 하나 이상 선택해 주세요.") List<@Valid PurchaseReturnItemRequest> items,
		@NotBlank(message = "매입 반품 사유를 입력해 주세요.")
		@Size(max = 1000, message = "매입 반품 사유는 1000자 이하로 입력해 주세요.") String reason
) {
}
