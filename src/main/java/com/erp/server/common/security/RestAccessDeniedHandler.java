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

// ********** 인증된 사용자의 권한이 부족하거나 CSRF 검증에 실패하여 접근이 거부된 경우 403 / FORBIDDEN 공통 오류 응답을 반환하기 위한 접근 거부 오류 처리 클래스 **********
// AccessDeniedHandler를 구현했기 때문에 접근 거부가 발생하면 Spring Security가 handle()을 호출한다.
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	// ErrorResponse 객체를 JSON으로 변환하여 응답 Body에 작성하기 위한 JsonMapper 객체
	private final JsonMapper jsonMapper;

	// ========== JSON 응답 변환에 사용할 JsonMapper를 주입받는 생성자 ==========
	public RestAccessDeniedHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	// ========== 접근이 거부된 요청을 403 공통 오류 응답으로 작성하는 메서드 ==========
	// AccessDeniedHandler에 선언된 handle()을 구현한다.
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {

		ErrorCode errorCode = ErrorCode.FORBIDDEN;

		// ErrorCode와 현재 Thread의 MDC에 저장된 traceId로 공통 오류 응답 객체를 만든다.
		ErrorResponse errorResponse = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(),
				MDC.get(TraceIdFilter.TRACE_ID_KEY));

		// Controller를 거치지 않으므로 HttpServletResponse에 HTTP 상태와 응답 형식을 직접 설정한다.
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		// JsonMapper.writeValue()가 ErrorResponse를 JSON으로 변환하여 응답 출력 Stream에 작성한다.
		jsonMapper.writeValue(response.getOutputStream(), errorResponse);
	}
}