package com.erp.server.common.user.dto;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;

// 사용자 관리 API에서 반환할 사용자 정보 DTO record
public record UserResponse(
        Long userId,
        String username,
        String userName,
        UserRole role,
        UserStatus status,
        LocalDateTime lastLoginAt,
        Long version
) {

    // AppUser Entity에서 사용자 관리 응답에 필요한 값만 가져온다.
    public static UserResponse from(AppUser appUser) {

        return new UserResponse(
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getUserName(),
                appUser.getRole(),
                appUser.getStatus(),
                appUser.getLastLoginAt(),
                appUser.getVersion()
        );
    }
}