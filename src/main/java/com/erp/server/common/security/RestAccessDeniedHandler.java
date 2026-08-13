package com.erp.server.common.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.erp.server.common.exception.ErrorCode;
import com.erp.server.common.filter.TraceIdFilter;
import com.erp.server.common.response.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// 인증된 사용자의 권한이 부족하거나 CSRF 검증에 실패하여 접근이 거부된 경우 403 / FORBIDDEN 공통 오류 응답을 반환하기 위한 접근 거부 오류 처리 클래스

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public RestAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // ********** 403이 발생했을 때 실행될 메서드 **********
    // request: 접근이 거부된 요청, response: 클라이언트에 보낼 응답, accessDeniedException: 접근이 거부된 원인 예외
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        // 기존 ErrorResponse 형식으로 403 오류 응답 생성
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(),
                MDC.get(TraceIdFilter.TRACE_ID_KEY));

        // HTTP 상태와 응답 형식 설정
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ErrorResponse 객체를 JSON으로 변환하여 응답 body 에 작성
        jsonMapper.writeValue(response.getOutputStream(),errorResponse);
    }
}