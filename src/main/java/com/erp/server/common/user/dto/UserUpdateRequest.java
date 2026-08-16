package com.erp.server.common.user.dto;

import com.erp.server.common.user.domain.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// 사용자명과 역할 수정 정보를 전달받기 위한 요청 DTO record
public record UserUpdateRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(max = 100, message = "사용자명은 100자 이하여야 합니다.")
        String userName,

        @NotNull(message = "사용자 역할은 필수입니다.")
        UserRole role,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}