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
 * {@code @Configuration}: 해당 클래스가 Spring 설정을 정의하는 클래스라는 것을 나타내는 annotation
 * {@code @EnableWebSecurity}: Spring Security의 웹 보안 기능을 활성화하고 웹 보안 설정을 구성할 수 있도록 하는 annotation
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/*
	 * RestAuthenticationEntryPoint와 RestAccessDeniedHandler는 @Component로 등록된 Spring Bean이므로, 
	 * securityFilterChain() 실행 시 Spring이 필요한 객체를 매개변수로 자동 주입한다.
	 * 
	 */
	
	/*
     * /api/** 요청에 적용되는 API 전용 SecurityFilterChain
     */ 
	@Bean
	@Order(1)
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			RestAuthenticationEntryPoint restAuthenticationEntryPoint, 
			RestAccessDeniedHandler restAccessDeniedHandler,
			CsrfTokenRepository csrfTokenRepository) throws Exception {

		/*
		 * authorizeHttpRequests: HTTP 요청별 접근 권한을 설정한다.
		 * authenticated(): 인증된(로그인된) 사용자만 해당 요청에 접근할 수 있도록 설정한다.
		 * 현재는 모든 요청에 인증을 요구하고, 로그인 API처럼 인증 없이 접근해야 하는 경로는 이후 permitAll()로 별도 허용한다.
		 */
		http	// /api/** 요청에만 이 SecurityFilterChain을 적용한다.
				.securityMatcher("/api/**") 
		
				.authorizeHttpRequests(authorize -> authorize

						// 로그인 전 CSRF 토큰 발급과 로그인 API는 비로그인 사용자도 접근할 수 있다.
		                .requestMatchers(
		                        "/api/v1/auth/csrf",
		                        "/api/v1/auth/login"
		                ).permitAll()

				        // 그 외 API는 로그인된 사용자만 접근할 수 있다.
				        .anyRequest().authenticated())
				
				// httpBasic.disable(): 매 요청마다 아이디와 비밀번호를 전달하는 HTTP Basic 인증은 사용하지 않고, 로그인 후 생성된 세션을 이용하는 세션 기반 인증을 사용한다.
				.httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic 인증은 사용하지 않는다.

				// sessionManagement(IF_REQUIRED): 로그인 등 세션이 필요한 경우에만 세션을 생성하고, 이후 요청에서는 기존 세션을 사용한다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				
				// API 요청을 로그인 후 Redirect하기 위해 저장하지 않는다.
	            .requestCache(requestCache -> requestCache.disable())

				// CSRF 보호 기능을 활성화한다. POST, PUT, PATCH, DELETE 등 상태를 변경하는 요청에서는 올바른 CSRF 토큰인지 검증하게 된다.
	            .csrf(csrf ->
                	csrf.csrfTokenRepository(csrfTokenRepository))

				 // API의 401/403 오류는 직접 만든 Handler에서 공통 오류 응답으로 처리한다.
				.exceptionHandling(exception -> exception
		                .authenticationEntryPoint(restAuthenticationEntryPoint)
		                .accessDeniedHandler(restAccessDeniedHandler)
		            );

		return http.build(); // 위 보안 규칙으로 SecurityFilterChain을 생성하고, @Bean을 통해 Spring Bean으로 등록한다.
	}
	
     // 로그인 비밀번호의 해시 생성 및 검증에 사용할 PasswordEncoder를 등록하기 위한 메서드
    @Bean
    public PasswordEncoder passwordEncoder() {
        // APP_USER.password_hash에 저장할 비밀번호는 BCrypt 방식으로 처리한다.
        return new BCryptPasswordEncoder();
    }
    
    // REST 로그인에서 AppUserDetailsService와 PasswordEncoder를 이용해 사용자 인증을 처리할 AuthenticationManager를 등록하기 위한 메서드
    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService appUserDetailsService, PasswordEncoder passwordEncoder) {

    	// 사용자 조회와 비밀번호 검증을 실제로 처리할 인증 객체
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(appUserDetailsService);

        authenticationProvider.setPasswordEncoder(passwordEncoder);

        // DaoAuthenticationProvider를 이용해 인증을 처리하는 AuthenticationManager를 생성해 반환한다.
        return new ProviderManager(authenticationProvider);
    }
    
    // 현재 Session에 CSRF 토큰을 저장하고 조회하기 위한 CsrfTokenRepository를 등록하기 위한 메서드
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {

        return new HttpSessionCsrfTokenRepository();
    }
}