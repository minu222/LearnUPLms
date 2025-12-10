package com.lms.mainpages.board.free;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FreeBoardListInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // /board/free 페이지로 들어오는 경우, 뷰에서 사용할 힌트만 attribute로 남긴다.
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/board/free")) {
            // 소문자로 통일 — 컨트롤러/리포지토리는 이 값을 그대로 쓰거나, API 요청 파라미터가 오면 toLowerCase로 처리
            request.setAttribute("boardCategory", "free");
        }
        return true;
    }
}
