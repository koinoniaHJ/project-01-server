package com.erp.server.common.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.server.common.auth.dto.LoginRequest;
import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.security.AppUserDetails;
import com.erp.server.common.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

// ********** 로그인 요청을 Spring Security 인증에 전달하고 인증 결과에 따라 최종 로그인 일시 또는 공통 인증 오류를 처리하기 위한 Service 클래스 **********
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;

    // ========== 로그인 아이디와 비밀번호로 사용자를 인증하고 최종 로그인 일시를 갱신하는 메서드 ==========
    @Transactional
    public Authentication authenticate(LoginRequest request) {

        // unauthenticated()는 아이디와 비밀번호를 담은 인증 전 Authentication 객체를 생성한다.
        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                );

        try {
            // AuthenticationManager.authenticate()가 DB 사용자 조회와 비밀번호 검증을 수행한다.
            Authentication authentication =
                    authenticationManager.authenticate(authenticationToken);

            // 인증 완료 후 principal에는 AppUserDetailsService가 반환한 AppUserDetails가 들어 있다.
            AppUserDetails currentUser =
                    (AppUserDetails) authentication.getPrincipal();

            // 전용 UPDATE 쿼리로 last_login_at만 변경하여 사용자 수정용 version은 유지한다.
            appUserRepository.updateLastLoginAt(
                    currentUser.getUserId(),
                    LocalDateTime.now()
            );

            return authentication;

        } catch (DisabledException exception) {
            // AppUserDetails.isEnabled()가 false인 INACTIVE 사용자는 403으로 변환한다.
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "사용 중지된 사용자입니다."
            );

        } catch (AuthenticationException exception) {
            // 사용자 존재 여부가 드러나지 않도록 아이디·비밀번호 오류를 같은 메시지로 처리한다.
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "아이디 또는 비밀번호가 일치하지 않습니다."
            );
        }
    }
}