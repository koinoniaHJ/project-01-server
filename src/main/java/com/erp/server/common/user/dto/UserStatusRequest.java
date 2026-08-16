package com.erp.server.common.user.dto;

import com.erp.server.common.user.domain.UserStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// 사용자 상태 변경 정보를 전달받기 위한 요청 DTO record
public record UserStatusRequest(

        @NotNull(message = "사용자 상태는 필수입니다.")
        UserStatus status,

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version

) {
}