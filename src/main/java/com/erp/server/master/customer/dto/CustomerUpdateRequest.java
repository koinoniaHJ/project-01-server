package com.erp.server.master.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// ********** 거래처 기본정보·기본 배송정보 변경값과 동시 수정 확인용 version을 전달받기 위한 요청 DTO record **********
// 동시 수정 확인용 version을 전달받는다. 사용 상태와 거래 상태는 각각 별도 API로 변경하므로 포함하지 않는다. 총미수금도 정산 업무에서만 갱신하므로 제외한다.
public record CustomerUpdateRequest(

        @NotBlank(message = "거래처명은 필수입니다.")
        @Size(max = 150, message = "거래처명은 150자 이하여야 합니다.")
        String customerName,

        @Size(max = 30, message = "대표 연락처는 30자 이하여야 합니다.")
        String phone,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @Size(max = 10, message = "사업장 우편번호는 10자 이하여야 합니다.")
        String postalCode,

        @Size(max = 500, message = "사업장 주소는 500자 이하여야 합니다.")
        String address,

        @Size(max = 300, message = "사업장 상세 주소는 300자 이하여야 합니다.")
        String addressDetail,

        @Size(max = 10, message = "배송지 우편번호는 10자 이하여야 합니다.")
        String deliveryPostalCode,

        @Size(max = 500, message = "배송지 주소는 500자 이하여야 합니다.")
        String deliveryAddress,

        @Size(max = 300, message = "배송지 상세 주소는 300자 이하여야 합니다.")
        String deliveryAddressDetail,

        @Size(max = 100, message = "수령인 이름은 100자 이하여야 합니다.")
        String recipientName,

        @Size(max = 30, message = "수령인 연락처는 30자 이하여야 합니다.")
        String recipientPhone,

        @Size(max = 2000, message = "거래처 메모는 2000자 이하여야 합니다.")
        String memo,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}