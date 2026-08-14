package com.erp.server.common.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.erp.server.common.auth.service.AuthService;
import com.erp.server.common.response.ApiResponse;
import com.erp.server.common.security.AppUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// CSRF 토큰 발급과 로그인 REST API 요청을 받아 사용자 인증 및 로그인 Session 생성을 처리하기 위한 Controller 클래스
@RestController // REST API 요청을 처리하고 반환값을 응답 본문(JSON 등)으로 전달하는 Controller로 지정
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CsrfTokenRepository csrfTokenRepository;

    // 인증 성공 정보를 HTTP Session에 저장하기 위해 사용한다.
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    // 현재 Session에서 사용할 CSRF 토큰을 반환하기 위한 메서드
    @GetMapping("/csrf")
    public ApiResponse<CsrfResponse> csrf(CsrfToken csrfToken) {

        return ApiResponse.success(CsrfResponse.from(csrfToken));
    }

    // 로그인 아이디와 비밀번호를 인증하고 로그인 Session을 생성하기 위한 메서드
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        // AuthService를 통해 실제 사용자 인증을 수행한다.
        Authentication authentication = authService.authenticate(request);

        // 로그인 전 CSRF 토큰 발급으로 Session이 만들어져 있으면 로그인 성공 시 Session ID를 변경하여 기존 Session ID를 그대로 사용하지 않는다.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        // 인증된 Authentication을 저장할 새로운 SecurityContext를 생성한다.
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        // 현재 요청에서도 인증된 사용자 정보를 사용할 수 있도록 설정한다.
        SecurityContextHolder.setContext(securityContext);

        // 이후 요청에서도 로그인 상태가 유지되도록 HTTP Session에 저장한다.
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        // 로그인 성공 전 사용하던 CSRF 토큰을 제거한다. 로그인 성공 후 GET /api/v1/auth/csrf를 다시 호출하여 새 토큰을 발급받는다.
        csrfTokenRepository.saveToken(null, httpRequest, httpResponse);

        // 인증 완료 Authentication에서 실제 로그인 사용자 정보를 가져온다.
        AppUserDetails appUserDetails = (AppUserDetails) authentication.getPrincipal();

        return ApiResponse.success(LoginResponse.from(appUserDetails));
    }
}