// src/main/java/dwacademy/mylms001/repository/CourseRepository.java
package com.lms.mainpages.repository;

import com.lms.mainpages.exam.entity.CourseInfoDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public class CourseRepository {

    private final JdbcTemplate jdbc;

    public CourseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 강의 등록 */
    public long insertCourse(
            int instructorId,
            String title,
            String description,
            String category,
            BigDecimal price,
            boolean isFree,
            String status,
            int studentCount,
            LocalDate expiryDate
    ) {
        final String sql =
                "INSERT INTO courses " +
                        " (instructor_id, title, description, category, price, is_free, status, student_count, expiry_date) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder kh = new GeneratedKeyHolder();

        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, instructorId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, category);
            if (price != null) ps.setBigDecimal(5, price);
            else ps.setNull(5, java.sql.Types.DECIMAL);
            ps.setBoolean(6, isFree);
            ps.setString(7, status);
            ps.setInt(8, studentCount);
            if (expiryDate != null) ps.setDate(9, Date.valueOf(expiryDate));
            else ps.setNull(9, java.sql.Types.DATE);
            return ps;
        }, kh);

        Number key = kh.getKey();
        return (key != null) ? key.longValue() : 0L;
    }

    /** 강의 존재(소유자 포함) */
    public boolean existsByIdAndInstructor(long courseId, int instructorId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE course_id = ? AND instructor_id = ?",
                Integer.class, courseId, instructorId
        );
        return n != null && n > 0;
    }

    /** 강의 삭제(소유자 포함) */
    public int deleteByIdAndInstructor(long courseId, int instructorId) {
        return jdbc.update(
                "DELETE FROM courses WHERE course_id = ? AND instructor_id = ?",
                courseId, instructorId
        );
    }

    /** 강의 존재 */
    public boolean existsById(long courseId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE course_id = ?",
                Integer.class, courseId
        );
        return n != null && n > 0;
    }

    /** 강의 삭제 */
    public int deleteById(long courseId) {
        return jdbc.update("DELETE FROM courses WHERE course_id = ?", courseId);
    }

    /** ✅ 강의 제목(SSOT) 조회: courses.title */
    public Optional<String> findTitleById(long courseId) {
        return jdbc.query(
                "SELECT title FROM courses WHERE course_id = ?",
                ps -> ps.setLong(1, courseId),
                rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty()
        );
    }

    public Optional<CourseInfoDTO> findTitleAndInstructorById(long courseId) {
        return jdbc.query(
                "SELECT title, instructor_id FROM courses WHERE course_id = ?",
                ps -> ps.setLong(1, courseId),
                rs -> rs.next() ? Optional.of(new CourseInfoDTO(
                        rs.getString("title"),
                        rs.getLong("instructor_id")
                )) : Optional.empty()
        );
    }
}
