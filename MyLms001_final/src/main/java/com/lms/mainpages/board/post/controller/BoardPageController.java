package com.lms.mainpages.board.post.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardPageController {

    private final JdbcTemplate jdbcTemplate;

    /** 자유게시판 상세 */
    @GetMapping("/free/{id}")
    public String freeDetail(@PathVariable Long id, Model model) {
        final String sql = """
            SELECT p.post_id AS id,
                   p.title,
                   p.content,
                   p.created_at,
                   u.name AS author_name,
                   COALESCE(u.role,'USER') AS author_role
              FROM project.posts p
              JOIN project.users u ON u.user_id = p.user_id
             WHERE p.post_id = ?
               AND UPPER(p.category) = 'FREE'
               AND p.is_deleted = 0
        """;

        Map<String,Object> post = jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            var m = new HashMap<String,Object>();
            m.put("id", rs.getLong("id"));
            m.put("title", rs.getString("title"));
            m.put("content", rs.getString("content"));
            var ts = rs.getTimestamp("created_at");
            m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
            m.put("authorName", rs.getString("author_name"));
            String role = rs.getString("author_role");
            m.put("authorRole", role);
            m.put("role", role);
            return m;
        }, id);

        if (post == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");

        model.addAttribute("post", post);
        model.addAttribute("typeSlug", "free");
        model.addAttribute("bodyFragment", "board/freeDetail :: content"); // ★ 경로 포함
        model.addAttribute("title", "자유게시판 상세");
        model.addAttribute("activeCategory", "board");
        model.addAttribute("showSidebar", true);
        model.addAttribute("userRole", "guest");
        return "layout";
    }

    /** 문의게시판 상세 */
    @GetMapping("/qna/{id}")
    public String qnaDetail(@PathVariable Long id, Model model) {
        final String sql = """
            SELECT p.post_id AS id,
                   p.title,
                   p.content,
                   p.created_at,
                   u.name AS author_name,
                   COALESCE(u.role,'USER') AS author_role
              FROM project.posts p
              JOIN project.users u ON u.user_id = p.user_id
             WHERE p.post_id = ?
               AND UPPER(p.category) = 'QNA'
               AND p.is_deleted = 0
        """;

        Map<String,Object> post = jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) return null;
            var m = new HashMap<String,Object>();
            m.put("id", rs.getLong("id"));
            m.put("title", rs.getString("title"));
            m.put("content", rs.getString("content"));
            var ts = rs.getTimestamp("created_at");
            m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
            m.put("authorName", rs.getString("author_name"));
            String role = rs.getString("author_role");
            m.put("authorRole", role);
            m.put("role", role);
            return m;
        }, id);

        if (post == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");

        model.addAttribute("post", post);
        model.addAttribute("typeSlug", "qna");
        model.addAttribute("bodyFragment", "board/qnaDetail :: content"); // ★ 경로 포함
        model.addAttribute("title", "문의게시판 상세");
        model.addAttribute("activeCategory", "board");
        model.addAttribute("showSidebar", true);
        model.addAttribute("userRole", "guest");
        return "layout";
    }

    /** 역할 코드 → 한글 라벨 */
    private String toKoreanRole(String role) {
        if (role == null) return "학생";
        return switch (role.toLowerCase()) {
            case "admin" -> "관리자";
            case "instructor" -> "강사";
            default -> "학생";
        };
    }

    @SuppressWarnings("unused")
    private String setup(Model model, String fragment, String title) {
        model.addAttribute("bodyFragment", fragment);
        model.addAttribute("title", title);
        model.addAttribute("activeCategory", "board");
        model.addAttribute("userRole", "guest");
        model.addAttribute("showSidebar", true);
        return "layout";
    }
}
