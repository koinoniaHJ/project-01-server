package com.erp.server.purchase.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// ********** DRAFT 발주 생성에 필요한 공급업체·발주 품목·메모를 전달받기 위한 요청 DTO record **********
// 발주 상태·총액·감사 정보·version은 서버에서 설정하고 클라이언트에서 받지 않는다.
public record PurchaseOrderCreateRequest(

		@NotNull(message = "공급업체 식별자는 필수입니다.")
		@Positive(message = "공급업체 식별자는 1 이상이어야 합니다.")
		Long supplierId,

		@NotEmpty(message = "발주 품목은 한 건 이상 필요합니다.")
		List<@Valid PurchaseOrderItemRequest> items,

		@Size(max = 2000, message = "발주 메모는 2000자 이하여야 합니다.")
		String memo
) {
}
