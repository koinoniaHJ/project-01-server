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

//********** 로그인하지 않았거나 Session이 만료된 사용자가 보호 API를 요청했을 때 401 / UNAUTHORIZED 공통 오류 응답을 반환하기 위한 인증 오류 처리 클래스 **********
//AuthenticationEntryPoint를 구현했기 때문에 미인증 요청이 발생하면 Spring Security가 commence()를 호출한다.
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	// ErrorResponse 객체를 JSON으로 변환하여 응답 Body에 작성하기 위한 JsonMapper 객체
	private final JsonMapper jsonMapper;

	// ========== JSON 응답 변환에 사용할 JsonMapper를 주입받는 생성자 ==========
	public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	// ========== 미인증 요청을 401 공통 오류 응답으로 작성하는 메서드 ==========
	// AuthenticationEntryPoint에 선언된 commence()를 구현한다.
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {

		ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

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