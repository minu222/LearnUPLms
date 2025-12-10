package com.lms.mainpages.api;

import com.lms.mainpages.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// CartApiController.java (위시리스트 전용으로 축소)
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {
    private final JdbcTemplate jdbc;

    @GetMapping
    public ResponseEntity<?> my(HttpSession session){
        User login = (User) session.getAttribute("loginUser");
        if (login == null) return ResponseEntity.status(401).body(Map.of("ok",false,"message","로그인 필요"));
        String sql = """
            SELECT c.course_id, c.title, c.description, c.price, c.is_free, c.created_at, vc.added_at
              FROM cart vc
              JOIN courses c ON c.course_id = vc.course_id
             WHERE vc.user_id = ?
             ORDER BY vc.added_at DESC
        """;
        return ResponseEntity.ok(jdbc.queryForList(sql, login.getUser_id()));
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Map<String,Object> body, HttpSession session){
        User login = (User) session.getAttribute("loginUser");
        if (login == null) return ResponseEntity.status(401).body(Map.of("ok",false,"message","로그인 필요"));
        long courseId = Long.parseLong(String.valueOf(body.get("courseId")));
        jdbc.update("INSERT IGNORE INTO cart(user_id, course_id, added_at) VALUES(?, ?, NOW())",
                login.getUser_id(), courseId);
        return ResponseEntity.ok(Map.of("ok",true));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> remove(@PathVariable long courseId, HttpSession session){
        User login = (User) session.getAttribute("loginUser");
        if (login == null) return ResponseEntity.status(401).body(Map.of("ok",false,"message","로그인 필요"));
        jdbc.update("DELETE FROM cart WHERE user_id=? AND course_id=?", login.getUser_id(), courseId);
        return ResponseEntity.ok(Map.of("ok",true));
    }
}
