// src/main/java/dwacademy/mylms001/board/comment/CommentApiController.java
package com.lms.mainpages.board.comment;

import com.lms.mainpages.board.comment.dto.CommentDto;
import com.lms.mainpages.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentApiController {

    private final CommentService service;

    /** 댓글 목록 */
    @GetMapping
    public List<CommentDto> list(@RequestParam long postId) {
        return service.list(postId);
    }

    /** 댓글 작성 */
    @PostMapping
    public ResponseEntity<?> write(@RequestBody WriteReq req, HttpServletRequest http) {
        Long userId = getLoginUserId(http);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        long id = service.write(req.postId, userId, req.content, req.parentCommentId);
        if (id <= 0) return ResponseEntity.badRequest().body(Map.of("ok", false));
        return ResponseEntity.ok(new IdRes(id));
    }

    /** 댓글 수정 (작성자만) */
    @PatchMapping("/{id}")
    public ResponseEntity<?> edit(@PathVariable long id, @RequestBody EditReq req, HttpServletRequest http) {
        Long userId = getLoginUserId(http);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean ok = service.edit(id, userId, req.content);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /** 댓글 삭제 (작성자만, soft delete) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id, HttpServletRequest http) {
        Long userId = getLoginUserId(http);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean ok = service.delete(id, userId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // ───────── helpers ─────────
    private Long getLoginUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;

        // 1) "loginUser" 세션 객체 우선
        Object o = s.getAttribute("loginUser");
        if (o instanceof User u) {
            // primitive(int/long)든 Long이든 모두 Long으로 박싱하여 반환
            try {
                return Long.valueOf(u.getUser_id());
            } catch (Exception ignore) {
                // 만약 User에 다른 게터명을 쓰는 경우 대비 (예: getId/getUserId)
                try {
                    // 리플렉션은 과하지만, 필요 시 아래처럼 바꿔도 됨:
                    // return Long.valueOf((Integer) User.class.getMethod("getUser_id").invoke(u));
                    // 여기서는 단순화 유지
                } catch (Exception ignored) {}
            }
        }

        // 2) "userId"만 저장하는 경우도 처리
        Object v = s.getAttribute("userId");
        if (v instanceof Number n) return n.longValue();
        try {
            return (v == null) ? null : Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ───────── DTOs ─────────
    @Data public static class WriteReq {
        private long postId;
        private String content;
        private Long parentCommentId; // 답글이면 부모 id, 아니면 null
    }
    @Data public static class EditReq { private String content; }
    @Data public static class IdRes { private final long id; }
}
