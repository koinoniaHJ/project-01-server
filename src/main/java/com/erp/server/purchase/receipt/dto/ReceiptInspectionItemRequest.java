package com.erp.server.purchase.receipt.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// ********** 입고 품목별 실제·정상·불합격 수량과 검수 메모·LOT 전체 구성을 전달하기 위한 요청 DTO record **********
public record ReceiptInspectionItemRequest(
		@NotNull(message = "입고 품목 식별자가 필요합니다.") @Positive(message = "입고 품목 식별자가 올바르지 않습니다.") Long receiptItemId,
		@NotNull(message = "실제 입고 수량을 입력해 주세요.") @DecimalMin(value = "0.000", message = "실제 입고 수량은 0 이상이어야 합니다.") @Digits(integer = 16, fraction = 3, message = "실제 입고 수량은 소수점 셋째 자리까지 입력할 수 있습니다.") BigDecimal actualQuantity,
		@NotNull(message = "정상 입고 수량을 입력해 주세요.") @DecimalMin(value = "0.000", message = "정상 입고 수량은 0 이상이어야 합니다.") @Digits(integer = 16, fraction = 3, message = "정상 입고 수량은 소수점 셋째 자리까지 입력할 수 있습니다.") BigDecimal normalQuantity,
		@NotNull(message = "불합격 수량을 입력해 주세요.") @DecimalMin(value = "0.000", message = "불합격 수량은 0 이상이어야 합니다.") @Digits(integer = 16, fraction = 3, message = "불합격 수량은 소수점 셋째 자리까지 입력할 수 있습니다.") BigDecimal rejectedQuantity,
		@Size(max = 1000, message = "검수 메모는 1000자 이하로 입력해 주세요.") String note,
		@NotNull(message = "입고 LOT 목록이 필요합니다.") List<@Valid ReceiptInspectionLotRequest> lots
) {
}
