package com.erp.server.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

// 로그인 아이디로 APP_USER를 조회하고, 조회 결과를 AppUserDetails로 만들어 Spring Security에 반환하는 사용자 조회 Service
@Service		// 해당 클래스를 Service 계층의 Spring Bean으로 등록하는 annotation
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    // Spring이 AppUserRepository Bean을 생성자를 통해 주입한다.
    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 로그인 아이디로 APP_USER 한 명을 조회하고 없으면 인증 예외를 발생시킨다.
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // DB Entity를 Spring Security가 사용할 UserDetails 형태로 변환한다.
        return AppUserDetails.from(appUser);
    }
}