package com.erp.server.common.user.dto;

import com.erp.server.common.user.domain.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ********** 신규 사용자 등록에 필요한 로그인 아이디·비밀번호·사용자명·역할을 전달받기 위한 요청 DTO record **********
public record UserCreateRequest(

        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(max = 50, message = "로그인 아이디는 50자 이하여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(max = 100, message = "사용자명은 100자 이하여야 합니다.")
        String userName,

        @NotNull(message = "사용자 역할은 필수입니다.")
        UserRole role

) {
}