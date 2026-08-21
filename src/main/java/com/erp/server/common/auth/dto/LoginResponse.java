package com.erp.server.common.auth.dto;

import com.erp.server.common.security.AppUserDetails;
import com.erp.server.common.user.domain.UserRole;

// ********** 로그인 성공 후 클라이언트에 필요한 사용자 식별자·사용자명·역할을 반환하기 위한 로그인 응답 DTO record **********
public record LoginResponse(
        Long userId,
        String userName,
        UserRole role
) {

    // ========== 인증된 사용자 정보를 LoginResponse로 변환하는 정적 팩토리 메서드 ==========
    public static LoginResponse from(AppUserDetails appUserDetails) {

        return new LoginResponse(
                appUserDetails.getUserId(),
                appUserDetails.getUserName(),
                appUserDetails.getRole()
        );
    }
}