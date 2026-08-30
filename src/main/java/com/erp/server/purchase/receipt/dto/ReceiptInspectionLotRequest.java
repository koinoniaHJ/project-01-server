package com.erp.server.purchase.receipt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ********** 정상 입고 수량을 구성하는 공급업체 LOT·사용기한·LOT별 수량을 전달하기 위한 요청 DTO record **********
public record ReceiptInspectionLotRequest(
		@Size(max = 100, message = "공급업체 LOT 번호는 100자 이하로 입력해 주세요.") String supplierLotNumber,
		@NotNull(message = "LOT 사용기한을 입력해 주세요.") LocalDate expiryDate,
		@NotNull(message = "LOT 정상 수량을 입력해 주세요.")
		@DecimalMin(value = "0.000", inclusive = false, message = "LOT 정상 수량은 0보다 커야 합니다.")
		@Digits(integer = 16, fraction = 3, message = "LOT 정상 수량은 소수점 셋째 자리까지 입력할 수 있습니다.") BigDecimal normalQuantity
) {
}
