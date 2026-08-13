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

/**
 * 각 API 요청마다 고유한 traceId를 생성해 요청 처리 동안 유지하고, 
 * 오류 응답과 서버 로그에 동일한 값을 기록하여 오류가 발생한 요청을 추적하기 위함
 *
 * {@code @Component}: TraceIdFilter를 Spring Bean으로 등록한다.
 * OncePerRequestFilter: TraceIdFilter가 Servlet Filter로 동작하도록 하고, 하나의 HTTP 요청에서 Filter 로직이 한 번 실행되도록 지원한다.
 * 
 * Spring Boot는 여러 Filter의 실행 순서를 지정할 때 @Order annotation 또는 Ordered 인터페이스를 사용할 수 있도록 지원한다.
 * {@code @Order(Ordered.HIGHEST_PRECEDENCE)}: 해당 Filter에 가장 높은 우선순위를 부여하여 다른 Filter보다 먼저 실행되도록 설정한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

     // MDC에 traceId를 저장하고 가져올 때 사용하는 key
    public static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
    		throws ServletException, IOException {

        /*
         * 요청 하나를 식별하기 위한 traceId 생성
         * UUID 예: 550e8400-e29b-41d4-a716-446655440000
         * "-" 제거 후 앞 16자리 사용 예: 550e8400e29b41d4
         */
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        //현재 요청을 처리하는 동안 사용할 수 있도록 MDC에 traceId 저장
        MDC.put(TRACE_ID_KEY, traceId);

        try {
        	// 현재 Filter 이후의 다음 Filter 및 Controller 등으로 요청 처리를 계속 진행한다.
            filterChain.doFilter(request, response);
        } finally {
            // 요청 처리가 끝나면 MDC에서 traceId 제거
        	// SLF4J MDC는 현재 요청을 처리하는 스레드에 값을 저장하므로, 스레드가 재사용될 때 이전 traceId가 남지 않도록 요청 처리 후 제거한다.
            MDC.remove(TRACE_ID_KEY);
        }
    }
}