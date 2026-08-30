package com.erp.server.purchase.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 발주 취소 사유·공급업체 취소 확인 여부와 동시 처리 확인용 version을 전달받기 위한 요청 DTO record **********
public record PurchaseOrderCancelRequest(

		@NotBlank(message = "발주 취소 사유는 필수입니다.")
		@Size(max = 1000, message = "발주 취소 사유는 1000자 이하여야 합니다.")
		String reason,

		@NotNull(message = "공급업체 취소 확인 여부는 필수입니다.")
		Boolean supplierCancelConfirmed,

		@NotNull(message = "version은 필수입니다.")
		@PositiveOrZero(message = "version은 0 이상이어야 합니다.")
		Long version
) {
}
