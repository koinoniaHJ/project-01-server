package com.erp.server.common.auth.dto;

import org.springframework.security.web.csrf.CsrfToken;

// 현재 Session의 CSRF 토큰을 클라이언트에 반환하기 위한 응답 DTO record
public record CsrfResponse(
        String token
) {

    // Spring Security의 CsrfToken에서 실제 토큰 값만 응답 DTO로 변환한다.
    public static CsrfResponse from(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }
}