package com.erp.server.purchase.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** DRAFT 발주의 공급업체·품목·메모 변경값과 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
public record PurchaseOrderUpdateRequest(

		@NotNull(message = "공급업체 식별자는 필수입니다.")
		@Positive(message = "공급업체 식별자는 1 이상이어야 합니다.")
		Long supplierId,

		@NotEmpty(message = "발주 품목은 한 건 이상 필요합니다.")
		List<@Valid PurchaseOrderItemRequest> items,

		@Size(max = 2000, message = "발주 메모는 2000자 이하여야 합니다.")
		String memo,

		@NotNull(message = "version은 필수입니다.")
		@PositiveOrZero(message = "version은 0 이상이어야 합니다.")
		Long version
) {
}
