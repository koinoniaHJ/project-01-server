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
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import com.erp.server.common.security.AppUserDetailsService;
import com.erp.server.common.security.RestAccessDeniedHandler;
import com.erp.server.common.security.RestAuthenticationEntryPoint;

/**
 * Spring Security에서 세션 기반 인증과 요청 접근 제어, CSRF 검증에 대한 기본 보안 설정을 구성하기 위한 설정 클래스
 *
 * {@code @Configuration}       Spring 설정 정보를 정의하는 클래스임을 나타내는 어노테이션
 * {@code @EnableWebSecurity}   Spring Security의 웹 보안 기능을 활성화하고, 웹 보안 설정을 구성할 수 있도록 하는 어노테이션
 *
 * 서버 시작 시 {@code @Configuration}으로 설정 클래스로 등록된 SecurityConfig.java에서 
 * {@code @Bean} 메서드를 통해 SecurityFilterChain을 생성·등록한다.
 * 이후 HTTP 요청마다 SecurityFilterChain이 먼저 실행되어 인증·인가·CSRF 검사를 수행한 뒤, 통과한 요청만 Controller로 전달된다.
 *
 * HttpSecurity http: SecurityFilterChain에 적용할 보안 규칙을 설정하는 객체
 * SecurityFilterChain: HttpSecurity의 보안 설정을 http.build()로 생성한 필터 체인
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	// ********** HTTP 요청 보안 규칙을 구성하고 SecurityFilterChain을 생성하는 메서드 **********
	@Bean
	@Order(1)
	public SecurityFilterChain securityFilterChain(HttpSecurity http, RestAuthenticationEntryPoint restAuthenticationEntryPoint, RestAccessDeniedHandler restAccessDeniedHandler, 
			CsrfTokenRepository csrfTokenRepository) throws Exception {

		http.securityMatcher("/api/**") 								// "/api/**" 요청에만 이 SecurityFilterChain을 적용한다. 
				.authorizeHttpRequests(authorize -> authorize			// HTTP 요청별 접근 권한을 설정한다.
		                .requestMatchers(								// requestMatchers(): 요청에 적용할 권한 규칙을 지정
		                		"/api/v1/auth/csrf", 
		                		"/api/v1/auth/login"
		                ).permitAll()									// 로그인 전 CSRF 토큰 발급과 로그인 API는 비로그인 사용자도 접근할 수 있다.
		                .requestMatchers(
		                		"/api/v1/users", 
		                		"/api/v1/users/**"
		                ).hasRole("ADMIN")								// 사용자 관리 API는 ADMIN 역할만 접근할 수 있다.
				        .anyRequest().authenticated())					// 그 외 API는 인증된(로그인된) 사용자만 접근할 수 있다.
				
																		// HTTP Basic 인증: 매 요청마다 아이디와 비밀번호를 Authorization 헤더에 담아서 보내는 방식
				.httpBasic(httpBasic -> httpBasic.disable()) 			// HTTP Basic 인증 비활성화

				// 세션이 필요한 경우에만 생성하고, 기존 세션이 있으면 재사용하도록 설정한다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				
				// 로그인 전 요청을 저장했다가 로그인 후 Redirect하지 않는다.
	            .requestCache(requestCache -> requestCache.disable())

				// CSRF 보호 기능을 활성화한다. POST, PUT, PATCH, DELETE 등 상태를 변경하는 요청에서는 올바른 CSRF 토큰인지 검증하게 된다.
	            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))

				 // API의 401/403 오류는 직접 만든 Handler에서 공통 오류 응답으로 처리한다.
				.exceptionHandling(exception -> exception
		                .authenticationEntryPoint(restAuthenticationEntryPoint)
		                .accessDeniedHandler(restAccessDeniedHandler));

		return http.build(); // 위에서 설정한 보안 규칙을 적용한 SecurityFilterChain을 생성해 반환한다.
	}
	
    // ********** 비밀번호 해시 생성과 로그인 시 비밀번호 검증에 사용할 PasswordEncoder를 생성하기 위한 메서드 **********
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호를 BCrypt 방식으로 해시 처리하고, 로그인 시 입력 비밀번호와 해시값을 비교한다.
        return new BCryptPasswordEncoder();
    }
    
    // ********** 로그인 요청에서 AppUserDetailsService로 사용자를 조회하고 PasswordEncoder로 비밀번호를 검증하는 AuthenticationManager를 생성하기 위한 메서드 **********
    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService appUserDetailsService, PasswordEncoder passwordEncoder) {

    	// AppUserDetailsService로 사용자를 조회하고 PasswordEncoder로 비밀번호를 검증할 DaoAuthenticationProvider를 생성
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(appUserDetailsService);

        // 로그인 비밀번호 검증에 사용할 PasswordEncoder를 설정
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        // DaoAuthenticationProvider를 이용해 실제 인증을 수행할 AuthenticationManager를 생성해 반환
        return new ProviderManager(authenticationProvider);
    }
    
    // ********** CSRF 토큰을 HTTP Session에 저장하고 조회할 CsrfTokenRepository를 생성하기 위한 메서드 **********
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {

    	// CSRF 토큰을 HTTP Session에 저장하는 구현체를 반환
        return new HttpSessionCsrfTokenRepository();
    }
}