package com.erp.server.common.auth.dto;

import com.erp.server.common.security.AppUserDetails;
import com.erp.server.common.user.domain.UserRole;

// 로그인 성공 후 현재 로그인 사용자의 기본 정보를 반환하기 위한 로그인 응답 DTO record
public record LoginResponse(
        Long userId,
        String userName,
        UserRole role
) {

    // 인증된 사용자 정보에서 로그인 응답에 필요한 값만 가져온다.
    public static LoginResponse from(AppUserDetails appUserDetails) {

        return new LoginResponse(
                appUserDetails.getUserId(),
                appUserDetails.getUserName(),
                appUserDetails.getRole()
        );
    }
}