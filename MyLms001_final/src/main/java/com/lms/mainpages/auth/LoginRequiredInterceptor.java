package com.lms.mainpages.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 비로그인 접근 차단용 인터셉터.
 * - 로그인 성공 시 세션 키 "userId"가 (양수)로 존재해야 통과
 * - 로그인/정적/에러 경로는 항상 통과 (무한 리다이렉트 방지)
 */
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        final String uri = req.getRequestURI();

        // 로그인 화면, 정적 리소스, 에러 페이지는 통과
        if (uri.startsWith("/login") || uri.startsWith("/user/login")
                || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.startsWith("/webjars/") || "/favicon.ico".equals(uri) || uri.startsWith("/error")) {
            return true;
        }

        // ✅ userId "양수"일 때만 로그인으로 인정 (0/null/음수/파싱실패 → 비로그인)
        if (isLoggedIn(req)) return true;

        // 비로그인 → 로그인 페이지로 리다이렉트 (원래 가려던 주소 보존)
        String target = uri + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        res.sendRedirect("/login?redirect=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
        return false;
    }

    private boolean isLoggedIn(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object v = s.getAttribute("userId");
        if (v instanceof Number n) return n.longValue() > 0;
        if (v instanceof String str) {
            try { return Long.parseLong(str.trim()) > 0; } catch (Exception ignore) {}
        }
        return false;
    }
}
