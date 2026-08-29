package com.erp.server.master.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 창고 기본정보·주소 변경값과 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
// 창고 코드는 변경할 수 없고 사용 상태는 별도 API로 변경하므로 수정 요청에 포함하지 않는다.
public record WarehouseUpdateRequest(

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
		String memo,

		@NotNull(message = "version은 필수입니다.")
		@PositiveOrZero(message = "version은 0 이상이어야 합니다.")
		Long version

) {
}
