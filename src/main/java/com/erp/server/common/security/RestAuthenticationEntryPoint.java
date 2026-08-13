package com.erp.server.common.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.filter.TraceIdFilter;
import com.erp.server.common.response.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// 로그인하지 않았거나 세션이 만료된 사용자가 보호 API를 요청했을 때 401 / UNAUTHORIZED 공통 오류 응답을 반환하기 위한 인증 오류 처리 클래스

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // ********** 401이 발생했을 때 실행될 메서드 **********
    // request: 인증이 필요한 요청, response: 클라이언트에 보낼 응답, authException: 인증되지 않은 상태와 관련된 예외
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        
        // 기존 ErrorResponse 형식으로 401 오류 응답 생성
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), MDC.get(TraceIdFilter.TRACE_ID_KEY));
        
        // HTTP 상태와 응답 형식 설정
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ErrorResponse 객체를 JSON으로 변환하여 응답 body 에 작성
        jsonMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}