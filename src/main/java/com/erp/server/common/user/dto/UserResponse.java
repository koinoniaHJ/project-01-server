package com.erp.server.common.user.dto;

import java.time.LocalDateTime;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;

// ********** 사용자 관리 API에서 비밀번호 해시를 제외한 사용자 정보와 최신 version을 반환하기 위한 응답 DTO record **********
public record UserResponse(
        Long userId,
        String username,
        String userName,
        UserRole role,
        UserStatus status,
        LocalDateTime lastLoginAt,
        Long version
) {

    // ========== AppUser Entity를 UserResponse로 변환하는 정적 팩토리 메서드 ==========
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