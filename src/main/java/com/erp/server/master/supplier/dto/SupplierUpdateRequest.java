package com.erp.server.master.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 공급업체 기본정보·주소 변경값과 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
// 동시 수정 확인용 version을 전달받는다. 공급업체 사용 상태는 별도 API로 변경하므로 포함하지 않는다.
public record SupplierUpdateRequest(

        @NotBlank(message = "공급업체명은 필수입니다.")
        @Size(max = 150, message = "공급업체명은 150자 이하여야 합니다.")
        String supplierName,

        @Size(max = 30, message = "대표 연락처는 30자 이하여야 합니다.")
        String phone,

        @NotBlank(message = "발주 이메일은 필수입니다.")
        @Email(message = "발주 이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "발주 이메일은 255자 이하여야 합니다.")
        String email,

        @Size(max = 10, message = "사업장 우편번호는 10자 이하여야 합니다.")
        String postalCode,

        @Size(max = 500, message = "사업장 주소는 500자 이하여야 합니다.")
        String address,

        @Size(max = 300, message = "사업장 상세 주소는 300자 이하여야 합니다.")
        String addressDetail,

        @Size(max = 2000, message = "공급업체 메모는 2000자 이하여야 합니다.")
        String memo,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}
