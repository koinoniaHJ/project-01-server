package com.erp.server.master.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ********** 신규 창고 등록에 필요한 창고명·주소와 메모를 전달받기 위한 요청 DTO record **********
// 창고 코드는 서버에서 자동 생성하고 사용 상태·감사 정보·version은 클라이언트에서 받지 않는다.
public record WarehouseCreateRequest(

		@NotBlank(message = "창고명은 필수입니다.")
		@Size(max = 150, message = "창고명은 150자 이하여야 합니다.")
		String warehouseName,

		@Size(max = 10, message = "창고 우편번호는 10자 이하여야 합니다.")
		String postalCode,

		@Size(max = 500, message = "창고 주소는 500자 이하여야 합니다.")
		String address,

		@Size(max = 300, message = "창고 상세 주소는 300자 이하여야 합니다.")
		String addressDetail,

		@Size(max = 2000, message = "창고 메모는 2000자 이하여야 합니다.")
		String memo

) {
}
