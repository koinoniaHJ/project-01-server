package com.erp.server.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.erp.server.common.user.domain.AppUser;
import com.erp.server.common.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

// ********** 로그인 아이디로 APP_USER를 조회하고 Spring Security가 사용할 AppUserDetails를 반환하기 위한 UserDetailsService 구현 클래스 **********
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	// ========== 로그인 아이디로 사용자를 조회하여 UserDetails로 반환하는 메서드 ==========
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		// AppUserRepository.findByUsername()은 Optional<AppUser>를 반환한다.
		// Optional.orElseThrow()는 사용자가 없을 때 인증용 UsernameNotFoundException을 발생시킨다.
		AppUser appUser = appUserRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		return AppUserDetails.from(appUser);
	}
}