package com.erp.server.common.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// ********** 로그인 후 변경된 사용자 상태와 역할을 보호 API 요청마다 DB에서 다시 확인하고 현재 인증 정보에 반영하기 위한 요청 검증 Filter 클래스 **********
public class CurrentUserRefreshFilter extends OncePerRequestFilter {

    private final AppUserDetailsService appUserDetailsService;
    private final RestAccessDeniedHandler accessDeniedHandler;

    // ========== 최신 사용자 조회 Service와 접근 거부 Handler를 전달받는 생성자 ==========
    public CurrentUserRefreshFilter(
            AppUserDetailsService appUserDetailsService,
            RestAccessDeniedHandler accessDeniedHandler) {

        this.appUserDetailsService = appUserDetailsService;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    // ========== 인증 사용자의 최신 상태·역할을 확인하고 Authentication을 갱신하는 Filter 메서드 ==========
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // SecurityContextHolder에서 Session으로 복원된 현재 Authentication을 조회한다.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // 비로그인 요청이나 프로젝트의 AppUserDetails가 아닌 인증은 다음 Filter로 전달한다.
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                        instanceof AppUserDetails currentUser)) {

            filterChain.doFilter(request, response);
            return;
        }

        AppUserDetails latestUser;

        try {
            // 기존 Session의 username으로 DB 사용자를 다시 조회하여 최신 상태와 역할을 가져온다.
            latestUser = (AppUserDetails) appUserDetailsService
                    .loadUserByUsername(currentUser.getUsername());

        } catch (UsernameNotFoundException exception) {
            rejectCurrentSession(request, response);
            return;
        }

        if (!latestUser.isEnabled()) {
            rejectCurrentSession(request, response);
            return;
        }

        // 비밀번호 재검증 없이 최신 사용자와 권한을 가진 인증 완료 Authentication을 생성한다.
        UsernamePasswordAuthenticationToken refreshedAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        latestUser,
                        null,
                        latestUser.getAuthorities()
                );

        // 요청 IP나 Session ID처럼 기존 인증에 들어 있던 요청 세부 정보는 유지한다.
        refreshedAuthentication.setDetails(authentication.getDetails());

        // 뒤의 AuthorizationFilter와 Controller가 최신 역할을 사용하도록 현재 인증 정보를 교체한다.
        SecurityContextHolder
                .getContext()
                .setAuthentication(refreshedAuthentication);

        filterChain.doFilter(request, response);
    }

    // ========== 존재하지 않거나 INACTIVE인 사용자의 Session을 무효화하고 403을 반환하는 메서드 ==========
    private void rejectCurrentSession(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        // 기존 공통 403 형식을 유지하기 위해 RestAccessDeniedHandler에 응답 작성을 맡긴다.
        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException(
                        "현재 사용자는 접근할 수 없습니다.")
        );
    }
}