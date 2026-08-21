package com.erp.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import com.erp.server.common.security.AppUserDetailsService;
import com.erp.server.common.security.CurrentUserRefreshFilter;
import com.erp.server.common.security.RestAccessDeniedHandler;
import com.erp.server.common.security.RestAuthenticationEntryPoint;

// ********** API의 Session 인증, CSRF, 공개 경로, RBAC, 최신 사용자 갱신 Filter와 공통 401·403 처리를 구성하기 위한 Spring Security 설정 클래스 **********
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// ========== API 요청에 적용할 SecurityFilterChain을 생성하여 Spring Bean으로 등록하는 메서드
	// ==========
	@Bean
	@Order(1)
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			RestAuthenticationEntryPoint restAuthenticationEntryPoint, RestAccessDeniedHandler restAccessDeniedHandler,
			CsrfTokenRepository csrfTokenRepository, AppUserDetailsService appUserDetailsService) throws Exception {

		http
				// securityMatcher()가 이 Chain의 적용 범위를 /api/**로 제한한다.
				.securityMatcher("/api/**")

				// 위에서 아래 순서로 경로별 접근 규칙을 확인한다.
				.authorizeHttpRequests(authorize -> authorize
						// CSRF 발급과 로그인은 비로그인 사용자도 호출할 수 있다.
						.requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login").permitAll()

						// 사용자 관리 API는 ROLE_ADMIN 권한만 허용한다.
						.requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")

						// 그 외 API는 로그인한 사용자만 허용한다.
						.anyRequest().authenticated())

				// 매 요청에 아이디와 비밀번호를 보내는 HTTP Basic 인증은 사용하지 않는다.
				.httpBasic(httpBasic -> httpBasic.disable())

				// 로그인처럼 필요한 시점에만 Session을 만들고 기존 Session이 있으면 재사용한다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

				// API 요청을 저장했다가 로그인 후 Redirect하는 Request Cache는 사용하지 않는다.
				.requestCache(requestCache -> requestCache.disable())

				// 상태 변경 요청의 CSRF 토큰을 HTTP Session에 저장된 토큰과 비교한다.
				.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))

				// 권한 검사 전에 DB의 최신 사용자 상태와 역할을 현재 Authentication에 반영한다.
				.addFilterBefore(new CurrentUserRefreshFilter(appUserDetailsService, restAccessDeniedHandler),
						AuthorizationFilter.class)

				// Security Filter에서 발생한 401·403을 프로젝트의 공통 오류 JSON으로 변환한다.
				.exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler));

		return http.build();
	}

	// ========== 비밀번호 해시 생성과 검증에 사용할 PasswordEncoder를 Spring Bean으로 등록하는 메서드 ==========
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// ========== DB 사용자 조회와 비밀번호 검증을 수행할 AuthenticationManager를 Spring Bean으로 등록하는 ==========
	@Bean
	public AuthenticationManager authenticationManager(AppUserDetailsService appUserDetailsService,
			PasswordEncoder passwordEncoder) {

		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(appUserDetailsService);

		authenticationProvider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(authenticationProvider);
	}

	// ========== CSRF 토큰을 HTTP Session에 저장할 CsrfTokenRepository를 Spring Bean으로 등록하는 ==========
	@Bean
	public CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}
}