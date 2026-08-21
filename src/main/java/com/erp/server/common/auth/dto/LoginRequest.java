package com.erp.server.common.auth.dto;

import jakarta.validation.constraints.NotBlank;

// ********** 로그인 아이디와 비밀번호를 JSON 요청 본문으로 전달받기 위한 로그인 요청 DTO record **********
public record LoginRequest(

        @NotBlank(message = "로그인 아이디는 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}