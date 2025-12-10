package com.lms.adminpages.dashboard.dao;

import com.lms.adminpages.dashboard.entity.Dashboard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardDao {

    private final JdbcTemplate jdbcTemplate;

    public DashboardDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Dashboard getDashboardData() {
        Dashboard dto = new Dashboard();

        // 1. 총 학생 수
        String sqlStudent = "SELECT COUNT(*) FROM users WHERE role = 'student' AND status = 'active'";
        dto.setStudentCount(jdbcTemplate.queryForObject(sqlStudent, Integer.class));

        // 2. 총 강사 수
        String sqlTeacher = "SELECT COUNT(*) FROM users WHERE role = 'instructor' AND status = 'active'";
        dto.setTeacherCount(jdbcTemplate.queryForObject(sqlTeacher, Integer.class));

        // 3. 개설 과정 수
        String sqlCourse = "SELECT COUNT(*) FROM courses WHERE status = 'published'";
        dto.setCourseCount(jdbcTemplate.queryForObject(sqlCourse, Integer.class));

        // 4. 평균 수료율
        String sqlCompletion = "SELECT ROUND((SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) " +
                "FROM course_progress";
        Double avgCompletion = jdbcTemplate.queryForObject(sqlCompletion, Double.class);
        dto.setAvgCompletion(avgCompletion != null ? avgCompletion : 0.0);

        // 5. 취소율
        String sqlCancel = "SELECT ROUND((SUM(CASE WHEN status IN ('refund_requested','refunded') THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) " +
                "FROM orders";
        Double cancelRate = jdbcTemplate.queryForObject(sqlCancel, Double.class);
        dto.setCancelRate(cancelRate != null ? cancelRate : 0.0);

        // 6. 이번 달 매출
        String sqlRevenue = "SELECT IFNULL(SUM(total_amount), 0) FROM orders " +
                "WHERE status = 'paid' AND YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())";
        Double revenue = jdbcTemplate.queryForObject(sqlRevenue, Double.class);
        dto.setMonthRevenue(revenue != null ? revenue : 0.0);

        return dto;
    }
}