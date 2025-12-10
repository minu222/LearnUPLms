package com.lms.mainpages.board.free;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class FreeBoardModelAdvice {

    @ModelAttribute("boardCommon")
    public Map<String, Object> boardCommon(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String category = (uri != null && uri.startsWith("/board/free")) ? "free" : "";
        return Map.of(
                "boardCategory", category,     // 뷰에서 active 탭 등에 사용
                "boardTitleKo", category.equals("free") ? "자유게시판" : ""
        );
    }
}
