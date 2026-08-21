package com.erp.server.common.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.server.common.auth.dto.CsrfResponse;
import com.erp.server.common.auth.dto.LoginRequest;
import com.erp.server.common.auth.dto.LoginResponse;
import com.erp.server.common.auth.dto.MeResponse;
import com.erp.server.common.auth.service.AuthService;
import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.security.AppUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// ********** CSRF 토큰 발급, REST 로그인, 현재 사용자 조회와 로그아웃 요청을 처리하기 위한 인증 Controller 클래스 **********
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CsrfTokenRepository csrfTokenRepository;

	// 인증 성공 정보를 다음 요청에서도 사용할 수 있도록 HTTP Session에 저장한다.
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	// 현재 HTTP Session과 인증 정보를 정리하는 Spring Security Logout Handler이다.
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	// ========== 현재 Session에서 사용할 CSRF 토큰을 반환하는 메서드 ==========
	@GetMapping("/csrf")
	public ApiResponse<CsrfResponse> csrf(CsrfToken csrfToken) {
		return ApiResponse.success(CsrfResponse.from(csrfToken));
	}

	// ========== 로그인 정보를 인증하고 인증 결과를 HTTP Session에 저장하는 메서드 ==========
	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {

		Authentication authentication = authService.authenticate(request);

		// 로그인 전 CSRF 발급으로 Session이 이미 있으면 Session 고정 공격을 막기 위해 ID를 변경한다.
		if (httpRequest.getSession(false) != null) {
			httpRequest.changeSessionId();
		}

		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

		securityContext.setAuthentication(authentication);

		// 현재 요청에서 인증 정보를 사용할 수 있도록 SecurityContextHolder에 저장한다.
		SecurityContextHolder.setContext(securityContext);

		// 이후 요청에서도 로그인 상태가 유지되도록 SecurityContext를 HTTP Session에 저장한다.
		securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

		// 인증 전 토큰은 폐기하고 로그인 후 GET /csrf에서 새 토큰을 발급받게 한다.
		csrfTokenRepository.saveToken(null, httpRequest, httpResponse);

		AppUserDetails appUserDetails = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(LoginResponse.from(appUserDetails));
	}

	// ========== 현재 Session의 최신 로그인 사용자 정보를 반환하는 메서드 ==========
	@GetMapping("/me")
	public ApiResponse<MeResponse> me(Authentication authentication) {

		// CurrentUserRefreshFilter가 DB의 최신 상태와 역할로 Authentication을 갱신한 뒤 전달한다.
		AppUserDetails appUserDetails = (AppUserDetails) authentication.getPrincipal();

		return ApiResponse.success(MeResponse.from(appUserDetails));
	}

	// ========== 현재 HTTP Session과 인증 정보를 정리하여 로그아웃하는 메서드 ==========
	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			Authentication authentication) {

		logoutHandler.logout(httpRequest, httpResponse, authentication);

		return ApiResponse.success(null);
	}
}