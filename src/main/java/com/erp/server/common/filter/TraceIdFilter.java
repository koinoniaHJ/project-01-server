package com.erp.server.common.filter;

import java.io.IOException;

import java.util.UUID;

import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// ********** API 요청마다 traceId를 생성하고 요청 처리 동안 오류 응답과 서버 로그에서 같은 값을 사용하기 위한 요청 추적 Filter 클래스 **********
// OncePerRequestFilter를 상속했기 때문에 doFilterInternal()을 구현하여 Spring의 요청 Filter로 동작할 수 있다.
// 가장 먼저 실행하여 뒤에서 발생하는 인증·인가 오류에도 traceId를 사용할 수 있게 한다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	// MDC에 traceId를 저장·조회·삭제할 때 같은 Key를 사용하도록 상수로 정의한다.
	public static final String TRACE_ID_KEY = "traceId";

	// ========== 요청별 traceId를 생성·저장·정리하는 Filter 메서드 ==========
	// OncePerRequestFilter가 요청을 받을 때 호출하는 메서드를 재정의한다.
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// UUID.randomUUID()가 UUID 객체를 생성하고, UUID.toString()이 문자열로 변환한다.
		// String.replace()로 하이픈을 제거하고, String.substring()으로 앞 16글자만 가져온다.
		String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

		// MDC.put()으로 현재 Thread의 MDC에 traceId를 저장한다.
		MDC.put(TRACE_ID_KEY, traceId);

		try {
			// FilterChain.doFilter()로 다음 Filter와 Controller에 요청을 전달한다.
			filterChain.doFilter(request, response);
		} finally {
			// 요청이 끝난 뒤 재사용되는 Thread에 이전 요청의 traceId가 남지 않도록 제거한다.
			MDC.remove(TRACE_ID_KEY);
		}
	}
}