package com.erp.server.common.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.erp.server.common.auth.dto.LoginRequest;
import com.erp.server.common.exception.BusinessException;
import com.erp.server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

// 로그인 요청 정보를 AuthenticationManager에 전달하여 실제 사용자 인증을 처리하기 위한 Service 클래스
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    // 로그인 아이디와 비밀번호를 이용하여 Spring Security 인증을 수행하기 위한 메서드
    public Authentication authenticate(LoginRequest request) {

        // 입력받은 로그인 아이디와 비밀번호를 인증 요청 정보에 담는다.
        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated( // username과 password를 담은 인증 전 Authentication 객체를 생성하는 메서드
                        request.username(),
                        request.password());
        try {

            // AuthenticationManager에 인증을 요청한다.
            return authenticationManager.authenticate(authenticationToken);

        } catch (DisabledException exception) {

            // INACTIVE 사용자는 로그인할 수 없다.
            throw new BusinessException(ErrorCode.FORBIDDEN, "사용 중지된 사용자입니다.");

        } catch (AuthenticationException exception) {

            // 존재하지 않는 로그인 아이디와 잘못된 비밀번호는 동일한 인증 실패 메시지로 처리한다.
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "아이디 또는 비밀번호가 일치하지 않습니다."
            );
        }
    }
}