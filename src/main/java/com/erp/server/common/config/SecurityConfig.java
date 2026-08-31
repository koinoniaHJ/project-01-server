package com.erp.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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

	// ========== API 요청에 적용할 SecurityFilterChain을 생성하여 Spring Bean으로 등록하는 메서드 ==========
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
						
						// 거래처 등록 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/customers").hasAnyRole("ADMIN", "OFFICE")
						
						// 거래처 기본정보 수정 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.PATCH, "/api/v1/customers/*").hasAnyRole("ADMIN", "OFFICE")
						
						// 거래처 사용 상태 변경 API는 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/customers/*/status").hasRole("ADMIN")
						
						// 거래처 거래 상태 변경 API는 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/customers/*/trade-status").hasRole("ADMIN")

						// 공급업체 등록 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/suppliers").hasAnyRole("ADMIN", "OFFICE")

						// 공급업체 기본정보 수정 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.PATCH, "/api/v1/suppliers/*").hasAnyRole("ADMIN", "OFFICE")

						// 공급업체 사용 상태 변경 API는 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/suppliers/*/status").hasRole("ADMIN")

						// 품목 등록 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/items").hasAnyRole("ADMIN", "OFFICE")

						// 품목 기본정보 수정 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.PATCH, "/api/v1/items/*").hasAnyRole("ADMIN", "OFFICE")

						// 품목 사용 상태 변경 API는 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/items/*/status").hasRole("ADMIN")

						// 품목 취급 공급업체 등록 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/items/*/suppliers").hasAnyRole("ADMIN", "OFFICE")

						// 품목 취급 공급업체 관계 해제 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.DELETE, "/api/v1/items/*/suppliers/*").hasAnyRole("ADMIN", "OFFICE")

						// 창고 등록 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/warehouses").hasAnyRole("ADMIN", "OFFICE")

						// 창고 기본정보 수정 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.PATCH, "/api/v1/warehouses/*").hasAnyRole("ADMIN", "OFFICE")

						// 창고 사용 상태 변경 API는 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/warehouses/*/status").hasRole("ADMIN")

						// 창고·품목별 안전재고 등록·변경 API는 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.PUT, "/api/v1/warehouse-items/*/*").hasRole("ADMIN")

						// 발주 등록·수정·삭제 API는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/purchase-orders").hasAnyRole("ADMIN", "OFFICE")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/purchase-orders/*").hasAnyRole("ADMIN", "OFFICE")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/purchase-orders/*").hasAnyRole("ADMIN", "OFFICE")

						// 발주 승인은 ADMIN 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/purchase-orders/*/approve").hasRole("ADMIN")

						// 승인 요청·발주 확정·이메일 재전송·발주 취소는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/purchase-orders/*/submit",
								"/api/v1/purchase-orders/*/order", "/api/v1/purchase-orders/*/email/resend",
								"/api/v1/purchase-orders/*/cancel").hasAnyRole("ADMIN", "OFFICE")

						// 발주 이메일 전송 이력은 단가·금액·이메일 정보 접근이 가능한 ADMIN과 OFFICE만 조회한다.
						.requestMatchers(HttpMethod.GET, "/api/v1/purchase-orders/*/email-history")
								.hasAnyRole("ADMIN", "OFFICE")

						// 입고 등록·창고 수정·검수 시작·저장·완료·취소 API는 ADMIN과 WAREHOUSE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/receipts",
								"/api/v1/receipts/*/start-inspection", "/api/v1/receipts/*/complete",
								"/api/v1/receipts/*/cancel").hasAnyRole("ADMIN", "WAREHOUSE")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/receipts/*")
								.hasAnyRole("ADMIN", "WAREHOUSE")
						.requestMatchers(HttpMethod.PUT, "/api/v1/receipts/*/inspection")
								.hasAnyRole("ADMIN", "WAREHOUSE")

						// 매입 반품 등록·수정은 공급업체 구매 업무를 담당하는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/purchase-returns")
								.hasAnyRole("ADMIN", "OFFICE")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/purchase-returns/*")
								.hasAnyRole("ADMIN", "OFFICE")

						// 매입 반품 완료·취소는 실제 재고를 처리하는 ADMIN과 WAREHOUSE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/purchase-returns/*/complete",
								"/api/v1/purchase-returns/*/cancel").hasAnyRole("ADMIN", "WAREHOUSE")

						// 주문 등록·수정·삭제·접수·취소 API는 판매 업무를 담당하는 ADMIN과 OFFICE 권한만 허용한다.
						.requestMatchers(HttpMethod.POST, "/api/v1/sales-orders",
								"/api/v1/sales-orders/*/register", "/api/v1/sales-orders/*/cancel")
								.hasAnyRole("ADMIN", "OFFICE")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/sales-orders/*")
								.hasAnyRole("ADMIN", "OFFICE")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/sales-orders/*")
								.hasAnyRole("ADMIN", "OFFICE")

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

	// ========== DB 사용자 조회와 비밀번호 검증을 수행할 AuthenticationManager를 Spring Bean으로 등록하는 메서드 ==========
	@Bean
	public AuthenticationManager authenticationManager(AppUserDetailsService appUserDetailsService,
			PasswordEncoder passwordEncoder) {

		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(appUserDetailsService);

		authenticationProvider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(authenticationProvider);
	}

	// ========== CSRF 토큰을 HTTP Session에 저장할 CsrfTokenRepository를 Spring Bean으로 등록하는 메서드 ==========
	@Bean
	public CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}
}
