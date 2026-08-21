package com.erp.server.common.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.domain.UserRole;
import com.erp.server.common.user.domain.UserStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// ********** DB에서 조회한 AppUser를 Spring Security가 인증과 권한 검사에 사용할 수 있는 사용자 정보로 변환하여 보관하기 위한 UserDetails 구현 클래스 **********
@RequiredArgsConstructor
public class AppUserDetails implements UserDetails {

    @Getter
    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String userName;

    @Getter
    private final UserRole role;
    private final UserStatus status;

    // ========== AppUser Entity를 AppUserDetails로 변환하는 정적 팩토리 메서드 ==========
    public static AppUserDetails from(AppUser appUser) {

        return new AppUserDetails(
                appUser.getUserId(),
                appUser.getUsername(),
                appUser.getPasswordHash(),
                appUser.getUserName(),
                appUser.getRole(),
                appUser.getStatus()
        );
    }

    // ========== 사용자 역할을 Spring Security 권한 목록으로 변환하여 반환하는 메서드 ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        // hasRole("ADMIN")이 확인하는 ROLE_ADMIN 형식에 맞춰 DB 역할 앞에 ROLE_을 붙인다.
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    // ========== 인증에 사용할 비밀번호 해시를 반환하는 메서드 ==========
    @Override
    public String getPassword() {
        return passwordHash;
    }

    // ========== 로그인 아이디를 반환하는 메서드 ==========
    @Override
    public String getUsername() {
        return username;
    }

    // Lombok이 username과 userName의 Getter 이름을 충돌로 판단할 수 있어 직접 구분한다.
    // ========== 사용자 표시 이름을 반환하는 메서드 ==========
    public String getUserName() {
        return userName;
    }

    // ========== 사용자가 로그인 가능한 활성 상태인지 반환하는 메서드 ==========
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}