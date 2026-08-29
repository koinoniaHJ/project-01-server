package com.erp.server.master.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ********** 신규 공급업체 등록에 필요한 공급업체 기본정보와 주소를 전달받기 위한 요청 DTO record **********
// 공급업체 등록 API가 입력받을 기본정보와 주소를 정의한다. 자동 생성 코드, 사용 상태, 감사 정보는 클라이언트에서 받지 않는다.
public record SupplierCreateRequest(

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
        String memo

) {
}
