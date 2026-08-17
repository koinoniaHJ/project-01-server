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

/**
 * 로그인 이후 변경된 사용자 상태와 역할을 매 API 요청에 반영한다.
 */
public class CurrentUserRefreshFilter extends OncePerRequestFilter {

    private final AppUserDetailsService appUserDetailsService;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public CurrentUserRefreshFilter(
            AppUserDetailsService appUserDetailsService,
            RestAccessDeniedHandler accessDeniedHandler) {

        this.appUserDetailsService = appUserDetailsService;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUserDetails currentUser)) {

            filterChain.doFilter(request, response);
            return;
        }

        AppUserDetails latestUser;

        try {
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

        UsernamePasswordAuthenticationToken refreshedAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        latestUser,
                        null,
                        latestUser.getAuthorities()
                );

        refreshedAuthentication.setDetails(authentication.getDetails());

        SecurityContextHolder
                .getContext()
                .setAuthentication(refreshedAuthentication);

        filterChain.doFilter(request, response);
    }

    private void rejectCurrentSession(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException("현재 사용자는 접근할 수 없습니다.")
        );
    }
}