// src/main/java/dwacademy/mylms001/controller/TeacherCourseApiController.java
package com.lms.mainpages.api;

import com.lms.mainpages.entity.User;
import com.lms.mainpages.exceptoin.NotFoundException;
import com.lms.mainpages.service.CourseService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/teacher/courses")
public class TeacherCourseApiController {

    private final JdbcTemplate jdbc;
    private final CourseService courseService;

    public TeacherCourseApiController(JdbcTemplate jdbc, CourseService courseService) {
        this.jdbc = jdbc;
        this.courseService = courseService;
    }

    /* ============================== 목록 ============================== */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "status", required = false) String status,
                                  @RequestParam(value = "q", required = false) String q,
                                  HttpSession session) {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        int instructorId = login.getUser_id();

        // UI => DB 상태값 정규화
        String dbStatus = normalizeStatus(status); // "published" / "closed" / ""(전체)

        StringBuilder sql = new StringBuilder(
                "SELECT course_id, instructor_id, title, description, category, price, " +
                        "       is_free, avg_rating, status, student_count, expiry_date, " + // live_id 제거
                        "       created_at, updated_at, deleted_at " +
                        "  FROM courses WHERE instructor_id = ?"
        );

        List<Object> args = new ArrayList<>();
        args.add(instructorId);

        if (StringUtils.hasText(dbStatus)) {
            sql.append(" AND status = ?");
            args.add(dbStatus);
        }

        if (StringUtils.hasText(q)) {
            sql.append(" AND (title LIKE ? OR description LIKE ?)");
            String like = "%" + q.trim() + "%";
            args.add(like);
            args.add(like);
        }

        sql.append(" ORDER BY updated_at DESC, created_at DESC");

        try {
            // 타입 배열 없이 varargs로 넘겨 드라이버 추론
            List<Map<String, Object>> list = jdbc.queryForList(sql.toString(), args.toArray());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("List query failed. sql={}, args={}", sql, args, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", "목록 조회 중 오류가 발생했습니다."));
        }
    }

    /* ============================== 수정 ============================== */
    @PatchMapping("/{courseId}")
    public ResponseEntity<?> update(@PathVariable("courseId") Long courseId,
                                    @RequestBody CourseUpdateRequest req,
                                    HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        final int instructorId = login.getUser_id();

        if (!StringUtils.hasText(req.getTitle()))  return badRequest("강의명을 입력하세요.");
        if (!StringUtils.hasText(req.getPeriod())) return badRequest("기간을 입력하세요.");

        final LocalDate expiry   = parseRightDate(req.getPeriod());
        final String    category = normalizeCategory(req.getCategory());
        final String    status   = Optional.ofNullable(normalizeStatus(req.getStatus()))
                .filter(StringUtils::hasText)
                .orElse("published");

        // 본인 소유 확인
        Integer owner = jdbc.query(
                "SELECT instructor_id FROM courses WHERE course_id = ?",
                ps -> ps.setLong(1, courseId),
                rs -> rs.next() ? rs.getInt(1) : null
        );
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "message", "강의를 찾을 수 없습니다."));
        }
        if (!Objects.equals(owner, instructorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("ok", false, "message", "수정 권한이 없습니다."));
        }

        final String sql = "UPDATE courses " +
                "SET title = ?, category = ?, status = ?, expiry_date = ?, updated_at = NOW() " +
                "WHERE course_id = ? AND instructor_id = ?";

        try {
            int updated = jdbc.update(con -> {
                var ps = con.prepareStatement(sql);
                int i = 1;
                ps.setString(i++, req.getTitle().trim());
                ps.setString(i++, category);
                ps.setString(i++, status);
                if (expiry != null) ps.setDate(i++, Date.valueOf(expiry));
                else                ps.setNull(i++, Types.DATE);
                ps.setLong(i++, courseId);
                ps.setInt(i++, instructorId);
                return ps;
            });

            if (updated < 1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("ok", false, "message", "수정에 실패했습니다."));
            }
            return ResponseEntity.ok(Map.of("ok", true, "id", courseId));
        } catch (Exception e) {
            log.error("Update failed. id={}, req={}", courseId, req, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", "수정 중 오류가 발생했습니다."));
        }
    }

    /* ============================== 삭제 ============================== */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyCourse(@PathVariable long id, HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int instructorId = login.getUser_id();

        // 서비스 레이어 사용 (소유 확인 + 실제 삭제 + 업로드 파일 정리)
        courseService.deleteCourseByTeacher(instructorId, id);
        return ResponseEntity.noContent().build();
    }

    /* ============================== 예외 핸들러 ============================== */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("참조 데이터가 있어 삭제할 수 없습니다.");
    }

    /* ============================== 헬퍼 ============================== */

    private static ResponseEntity<Map<String,Object>> badRequest(String msg) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", msg));
    }

    /** UI '정상/중지/전체' or DB 'published/closed' -> DB 값/공백 */
    private static String normalizeStatus(String s) {
        if (!StringUtils.hasText(s)) return "";
        String v = s.trim();
        if (equalsAnyIgnoreCase(v, "전체", "all"))       return "";
        if (equalsAnyIgnoreCase(v, "정상", "published")) return "published";
        if (equalsAnyIgnoreCase(v, "중지", "closed"))    return "closed";
        return v.toLowerCase();
    }

    /** 허용값: VOD / 개인강의 / 다수강의 (그 외는 VOD) */
    private static String normalizeCategory(String s) {
        if (!StringUtils.hasText(s)) return "VOD";
        String v = s.trim();
        if (equalsAnyIgnoreCase(v, "VOD", "vod")) return "VOD";
        if (equalsAnyIgnoreCase(v, "개인강의"))   return "개인강의";
        if (equalsAnyIgnoreCase(v, "다수강의"))   return "다수강의";
        return "VOD";
    }

    /** "YYYY-MM-DD ~ YYYY-MM-DD" -> 오른쪽 날짜 파싱 (오류면 null) */
    private static LocalDate parseRightDate(String period) {
        if (!StringUtils.hasText(period)) return null;
        String[] parts = period.split("~");
        String right = (parts.length == 2 ? parts[1] : period).trim();
        try { return LocalDate.parse(right); } catch (Exception ignore) { return null; }
    }

    private static boolean equalsAnyIgnoreCase(String s, String... opts) {
        for (String o : opts) if (s.equalsIgnoreCase(o)) return true;
        return false;
    }

    /* ====== 요청 DTO ====== */
    public static class CourseUpdateRequest {
        private String title;
        private String period;
        private String category;
        private String status;

        public CourseUpdateRequest() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override public String toString() {
            return "CourseUpdateRequest{title='%s', period='%s', category='%s', status='%s'}"
                    .formatted(title, period, category, status);
        }
    }
}
