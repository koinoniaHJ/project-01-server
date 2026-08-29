package com.erp.server.master.item.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// ********** 품목에 취급 공급업체 관계를 등록할 supplierId를 전달받기 위한 요청 DTO record **********
// itemId는 API 경로에서 전달받고 관계 식별자·등록 감사 정보는 서버에서 관리한다.
public record SupplierItemRequest(

		@NotNull(message = "공급업체 식별자는 필수입니다.")
		@Positive(message = "공급업체 식별자는 0보다 커야 합니다.")
		Long supplierId

) {
}
