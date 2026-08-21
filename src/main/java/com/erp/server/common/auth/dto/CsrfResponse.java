package com.erp.server.common.auth.dto;

import org.springframework.security.web.csrf.CsrfToken;

// ********** 현재 Session에서 사용할 CSRF 토큰 값을 클라이언트에 반환하기 위한 응답 DTO record **********
public record CsrfResponse(
        String token
) {

    // ========== CsrfToken에서 실제 토큰 값만 CsrfResponse로 변환하는 정적 팩토리 메서드 ==========
    public static CsrfResponse from(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }
}