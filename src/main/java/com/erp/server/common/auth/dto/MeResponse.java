package com.erp.server.common.auth.dto;

import com.erp.server.common.security.AppUserDetails;
import com.erp.server.common.user.domain.UserRole;

// 현재 로그인 사용자의 기본 정보를 반환하기 위한 응답 DTO record
public record MeResponse(
        Long userId,
        String username,
        String userName,
        UserRole role
) {

    // 인증된 사용자 정보에서 현재 사용자 조회 응답에 필요한 값만 가져온다.
    public static MeResponse from(AppUserDetails appUserDetails) {

        return new MeResponse(
                appUserDetails.getUserId(),
                appUserDetails.getUsername(),
                appUserDetails.getUserName(),
                appUserDetails.getRole()
        );
    }
}