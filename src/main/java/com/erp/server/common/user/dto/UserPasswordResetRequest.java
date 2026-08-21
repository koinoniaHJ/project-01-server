package com.erp.server.common.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// ********** 새 비밀번호와 동시 수정 확인용 version을 전달받기 위한 비밀번호 초기화 요청 DTO record **********
public record UserPasswordResetRequest(

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}