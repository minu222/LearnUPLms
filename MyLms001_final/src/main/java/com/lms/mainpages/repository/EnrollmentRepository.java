package com.lms.mainpages.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EnrollmentRepository {
    private final JdbcTemplate jdbc;
    public EnrollmentRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    public void insert(int studentId, long courseId, long orderId){
        jdbc.update("""
            INSERT INTO enrollments (order_id, student_id, course_id, enrolled_at)
            VALUES (?, ?, ?, NOW())
        """, orderId, studentId, courseId);
    }

    public boolean exists(int studentId, long courseId){
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollments WHERE student_id=? AND course_id=?",
                Integer.class, studentId, courseId
        );
        return n != null && n > 0;
    }

    public List<?> listByStudent(int studentId){
        String sql = """
            SELECT e.enrollment_id, e.enrolled_at, e.expired_at,
                   c.course_id, c.title, c.description, c.category, c.price, c.is_free,
                   u.name AS instructor_name
              FROM enrollments e
              JOIN courses c ON c.course_id = e.course_id
              LEFT JOIN users u ON u.user_id = c.instructor_id
             WHERE e.student_id = ?
             ORDER BY e.enrolled_at DESC
        """;
        return jdbc.queryForList(sql, studentId);
    }
}
