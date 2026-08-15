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


// DB에서 조회한 AppUser의 사용자 정보를 Spring Security가 인증과 권한 검사에 사용할 수 있는 UserDetails 형태로 변환하여 보관하는 사용자 정보 객체
@RequiredArgsConstructor	// Lombok이 final 필드를 매개변수로 받는 생성자를 자동으로 생성한다.
public class AppUserDetails implements UserDetails {

    @Getter
    private final Long userId;     	// 현재 로그인 사용자를 식별하는 사용자 PK
    private final String username;
    private final String passwordHash;
    private final String userName;
    @Getter
    private final UserRole role;
    private final UserStatus status;

    // AppUser Entity를 Spring Security에서 사용할 AppUserDetails로 변환한다.
    public static AppUserDetails from(AppUser appUser) {

        return new AppUserDetails(appUser.getUserId(), appUser.getUsername(), appUser.getPasswordHash(), appUser.getUserName(), 
        		appUser.getRole(), appUser.getStatus());
    }

    // GrantedAuthority 또는 이를 구현한 타입을 담은 Collection을 반환할 수 있다는 뜻
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
         
        // DB의 ADMIN / OFFICE / WAREHOUSE 역할을 ROLE_ADMIN / ROLE_OFFICE / ROLE_WAREHOUSE 권한으로 변환한다.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    // Lombok은 getUsername()과 getUserName()을 대소문자 구분 없이 같은 이름으로 판단하므로 직접 작성한다.
    public String getUserName() {
        return userName;
    }

    @Override
    public boolean isEnabled() {
    	// 계정 활성화 여부 확인: ACTIVE 사용자만 로그인할 수 있다.
    	return status == UserStatus.ACTIVE;
    }
    // 예전에는 UserDetails 구현 시 아래 상태 확인 메서드를 직접 구현해야 했지만, Spring Security 7부터는 기본 구현이 제공되어 필요한 메서드만 재정의하면 된다.
    /* 
    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 여부 확인
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠금 여부 확인
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호 만료 여부 확인
        return true;
    }
    */

}