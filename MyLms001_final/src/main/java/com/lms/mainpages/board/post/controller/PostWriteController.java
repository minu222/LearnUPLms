package com.lms.mainpages.board.post.controller;

import com.lms.mainpages.board.post.service.PostWriteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class PostWriteController {

    private final PostWriteService writeService;

    /** 자유게시판 글쓰기 페이지 */
    @GetMapping("/free/write")
    public String writePage(Model model) {
        model.addAttribute("typeSlug", "free");
        model.addAttribute("bodyFragment", "board/write :: content");
        model.addAttribute("title", "자유게시판 글쓰기 | MyLMS");
        model.addAttribute("activeCategory", "board");
        model.addAttribute("userRole", "guest");
        model.addAttribute("showSidebar", true);
        return "layout";
    }

    /** 자유게시판 저장 */
    @PostMapping("/free/write")
    public String submit(
            HttpServletRequest req,
            @RequestParam String title,
            @RequestParam String content
    ) {
        // 최소 검증
        if (title == null || title.isBlank()) return "redirect:/board/free/write?error=title";
        if (content == null || content.isBlank()) return "redirect:/board/free/write?error=content";

        Long userId = currentUserId(req); // 세션 없으면 1로 대체
        Long newId = writeService.write(title, content, "free", userId);
        return "redirect:/board/free/" + newId;
    }

    private Long currentUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object v = session.getAttribute("userId");
            if (v instanceof Number n) return n.longValue();
            if (v instanceof String s) { try { return Long.parseLong(s); } catch (NumberFormatException ignored) {} }
        }
        return 1L; // 임시(비로그인): 1번 사용자로 저장
    }
}
