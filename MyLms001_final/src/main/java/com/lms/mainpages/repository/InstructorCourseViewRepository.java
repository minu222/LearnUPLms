// src/main/java/dwacademy/mylms001/repository/InstructorCourseViewRepository.java
package com.lms.mainpages.repository;

import com.lms.mainpages.dto.CourseCardResponse;
import com.lms.mainpages.dto.CourseStudentItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class InstructorCourseViewRepository {

    private final JdbcTemplate jdbc;
    public InstructorCourseViewRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /* ===== 코스 카드 매퍼 ===== */
    private static final RowMapper<CourseCardResponse> CARD_MAPPER = (rs, n) ->
            new CourseCardResponse(
                    rs.getLong("course_id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("status"),
                    rs.getBigDecimal("price"),
                    rs.getObject("is_free") == null ? null : rs.getInt("is_free") == 1,
                    rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null,
                    rs.getTimestamp("created_at")
            );

    /** 강사별 강의 목록(옵션: 상태, 키워드) */
    public List<CourseCardResponse> findCardsByInstructor(int instructorId, String statusDb, String keyword) {
        StringBuilder sql = new StringBuilder("""
            SELECT course_id, title, category, status, price, is_free, expiry_date, created_at
              FROM courses
             WHERE instructor_id = ?
            """);
        List<Object> args = new ArrayList<>();
        args.add(instructorId);

        if (statusDb != null) { sql.append(" AND status = ? "); args.add(statusDb); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR description LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like); args.add(like);
        }
        sql.append(" ORDER BY course_id DESC ");
        return jdbc.query(sql.toString(), CARD_MAPPER, args.toArray());
    }

    /* ===== 수강생 리스트 매퍼 ===== */
    private static final RowMapper<CourseStudentItem> STU_MAPPER = (rs, n) ->
            new CourseStudentItem(
                    rs.getLong("user_id"),
                    rs.getString("photo"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("gender_ko"),
                    rs.getString("email"),
                    (Integer) rs.getObject("age"),
                    rs.getString("intro")
            );

    /** 코스별 수강생 페이지 */
    public List<CourseStudentItem> findStudents(long courseId, int offset, int limit) {
        String sql = """
            SELECT u.user_id,
                   COALESCE(u.profile_image, '/img/default-user.png') AS photo,
                   u.name,
                   u.nickname AS username,
                   CASE UPPER(COALESCE(u.gender,'')) WHEN 'MALE' THEN '남'
                        WHEN 'FEMALE' THEN '여' ELSE '기타' END AS gender_ko,
                   u.email,
                   CASE WHEN u.birth_day IS NULL THEN NULL
                        ELSE TIMESTAMPDIFF(YEAR, u.birth_day, CURDATE()) END AS age,
                   COALESCE(u.intro, '') AS intro
              FROM enrollments e
              JOIN users u ON u.user_id = e.student_id
             WHERE e.course_id = ?
             ORDER BY u.user_id DESC
             LIMIT ? OFFSET ?
            """;
        return jdbc.query(sql, STU_MAPPER, courseId, limit, offset);
    }

    public int countStudents(long courseId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollments WHERE course_id = ?",
                Integer.class, courseId
        );
    }
}

