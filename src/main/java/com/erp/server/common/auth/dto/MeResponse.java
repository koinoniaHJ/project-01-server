package com.erp.server.common.auth.dto;

import com.erp.server.common.security.AppUserDetails;
import com.erp.server.common.user.domain.UserRole;

// ********** 현재 로그인 사용자의 식별자·로그인 아이디·사용자명·최신 역할을 반환하기 위한 현재 사용자 응답 DTO record **********
public record MeResponse(
        Long userId,
        String username,
        String userName,
        UserRole role
) {

    // ========== 인증된 사용자 정보를 MeResponse로 변환하는 정적 팩토리 메서드 ==========
    public static MeResponse from(AppUserDetails appUserDetails) {

        return new MeResponse(
                appUserDetails.getUserId(),
                appUserDetails.getUsername(),
                appUserDetails.getUserName(),
                appUserDetails.getRole()
        );
    }
}