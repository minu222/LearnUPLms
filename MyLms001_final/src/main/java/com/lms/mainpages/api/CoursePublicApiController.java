// src/main/java/dwacademy/mylms001/controller/CoursePublicApiController.java
package com.lms.mainpages.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class CoursePublicApiController {

    private final JdbcTemplate jdbc;

    public CoursePublicApiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 공개 강의 목록 (로그인 불필요)
     * params:
     *   category=vod|personal|multi (또는 VOD/개인강의/다수강의)
     *   status=published|closed|all  (기본: published)
     *   q=검색어
     *   sort=latest|oldest|popular   (기본: latest)
     *   page=0.., size=1..100        (기본: 0, 20)
     */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false, defaultValue = "") String category,
                                  @RequestParam(required = false, defaultValue = "published") String status,
                                  @RequestParam(required = false, defaultValue = "") String q,
                                  @RequestParam(required = false, defaultValue = "latest") String sort,
                                  @RequestParam(required = false, defaultValue = "0") int page,
                                  @RequestParam(required = false, defaultValue = "20") int size) {

        final String dbCategory = mapCategory(category);        // "VOD"|"개인강의"|"다수강의"|""
        final String dbStatus   = normalizeStatus(status);      // "published"|"closed"|""
        final int    limit      = Math.max(1, Math.min(size, 100));
        final int    offset     = Math.max(0, page) * limit;

        StringBuilder sql = new StringBuilder(
                "SELECT c.course_id, c.title, c.description, c.category, c.price, c.is_free, " +
                        "       c.avg_rating, c.status, c.student_count, c.expiry_date, c.created_at, " +
                        "       u.name AS instructor_name " +
                        "  FROM courses c LEFT JOIN users u ON u.user_id = c.instructor_id " +
                        " WHERE 1=1"
        );
        List<Object> args = new ArrayList<>();

        if (StringUtils.hasText(dbStatus)) {
            sql.append(" AND c.status = ?");
            args.add(dbStatus);
        }
        if (StringUtils.hasText(dbCategory)) {
            sql.append(" AND c.category = ?");
            args.add(dbCategory);
        }
        if (StringUtils.hasText(q)) {
            sql.append(" AND (c.title LIKE ? OR c.description LIKE ?)");
            String like = "%" + q.trim() + "%";
            args.add(like);
            args.add(like);
        }

        sql.append(" ORDER BY ");
        switch (sort) {
            case "oldest"  -> sql.append("c.created_at ASC");
            case "popular" -> sql.append("c.avg_rating DESC, c.student_count DESC, c.created_at DESC");
            default        -> sql.append("c.created_at DESC");
        }
        sql.append(" LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        try {
            return ResponseEntity.ok(jdbc.queryForList(sql.toString(), args.toArray()));
        } catch (Exception e) {
            log.error("Public course list failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", "목록 조회 중 오류가 발생했습니다."));
        }
    }

    private static String normalizeStatus(String s) {
        if (!StringUtils.hasText(s)) return "published";
        String v = s.trim();
        if (equalsAnyIgnoreCase(v, "전체", "all"))       return "";
        if (equalsAnyIgnoreCase(v, "정상", "published")) return "published";
        if (equalsAnyIgnoreCase(v, "중지", "closed"))    return "closed";
        return v.toLowerCase();
    }

    /** UI 탭값 → DB 카테고리 매핑 */
    private static String mapCategory(String c) {
        if (!StringUtils.hasText(c)) return "";
        String v = c.trim().toLowerCase();
        if (v.equals("vod") || v.equalsIgnoreCase("VOD"))     return "VOD";
        if (v.equals("personal") || v.equals("개인강의"))      return "개인강의";
        if (v.equals("multi") || v.equals("다수강의"))        return "다수강의";
        // 다른 값이면 전체로 취급
        return "";
    }

    private static boolean equalsAnyIgnoreCase(String s, String... opts) {
        for (String o : opts) if (s.equalsIgnoreCase(o)) return true;
        return false;
    }
}

