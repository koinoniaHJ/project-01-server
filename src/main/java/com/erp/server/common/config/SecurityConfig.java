package com.erp.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
			RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {

		/*
		 * authorizeHttpRequests: HTTP 요청별 접근 권한을 설정한다.
		 * authenticated(): 인증된(로그인된) 사용자만 해당 요청에 접근할 수 있도록 설정한다.
		 * 현재는 모든 요청에 인증을 요구하고, 로그인 API처럼 인증 없이 접근해야 하는 경로는 이후 permitAll()로 별도 허용한다.
		 */
		http	// /api/** 요청에만 이 SecurityFilterChain을 적용한다.
				.securityMatcher("/api/**") 
		
				// 모든 API 요청은 인증된 사용자만 접근할 수 있다.
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				
				// httpBasic.disable(): 매 요청마다 아이디와 비밀번호를 전달하는 HTTP Basic 인증은 사용하지 않고, 로그인 후 생성된 세션을 이용하는 세션 기반 인증을 사용한다.
				.httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic 인증은 사용하지 않는다.

				// sessionManagement(IF_REQUIRED): 로그인 등 세션이 필요한 경우에만 세션을 생성하고, 이후 요청에서는 기존 세션을 사용한다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				
				// API 요청을 로그인 후 Redirect하기 위해 저장하지 않는다.
	            .requestCache(requestCache -> requestCache.disable())

				// CSRF 보호 기능을 활성화한다. POST, PUT, PATCH, DELETE 등 상태를 변경하는 요청에서는 올바른 CSRF 토큰인지 검증하게 된다.
				.csrf(Customizer.withDefaults())

				 // API의 401/403 오류는 직접 만든 Handler에서 공통 오류 응답으로 처리한다.
				.exceptionHandling(exception -> exception
		                .authenticationEntryPoint(restAuthenticationEntryPoint)
		                .accessDeniedHandler(restAccessDeniedHandler)
		            );

		return http.build(); // 위 보안 규칙으로 SecurityFilterChain을 생성하고, @Bean을 통해 Spring Bean으로 등록한다.
	}
	/*
     * /api/** 이외의 요청에 적용되는 SecurityFilterChain
     * 현재는 Smoke Test를 위해 Spring Security의 기본 Form Login(/login)을 사용한다.
     */
    @Bean
    public SecurityFilterChain formLoginSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )

            // Smoke Test용 기본 Form Login
            .formLogin(Customizer.withDefaults())

            .httpBasic(httpBasic -> httpBasic.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            .csrf(Customizer.withDefaults());

        return http.build();
    }
	
	
}