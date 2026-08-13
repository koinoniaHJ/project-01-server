package com.erp.server.common.test;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 공통 Validation 처리를 확인하기 위한 테스트용 Request DTO
 *
 * name: null, "", 공백만 있는 문자열 허용 안 함
 *
 * quantity: null 허용 안 함, 최소값 1
 */
public record TestValidationRequest(

        @NotBlank
        String name,

        @NotNull
        @Min(1)
        Integer quantity

) {
}