package com.lms.mainpages.exam;

import com.lms.mainpages.exam.MockExamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MockExamResultRepository {

    private final JdbcTemplate jdbcTemplate;

    // 특정 학생 시험 결과 가져오기
    public List<MockExamResult> findByStudentId(Integer studentId) {
        String sql = "SELECT * FROM mock_exam_results WHERE student_id = ?";
        return jdbcTemplate.query(sql, new Object[]{studentId}, new MockExamResultRowMapper());
    }

    // ✅ 학생 + 강의 기준으로 시험 결과 가져오기
    public List<MockExamResult> findByStudentIdAndCourseId(Integer studentId, Integer courseId) {
        String sql = "SELECT * FROM mock_exam_results WHERE student_id = ? AND exam_id = ?";
        return jdbcTemplate.query(sql, new Object[]{studentId, courseId}, new MockExamResultRowMapper());
    }

    // RowMapper 정의
    private static class MockExamResultRowMapper implements RowMapper<MockExamResult> {
        @Override
        public MockExamResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            MockExamResult exam = new MockExamResult();
            exam.setResultId(rs.getInt("result_id"));
            exam.setExamId(rs.getInt("exam_id"));
            exam.setCourseTitle(rs.getString("course_title"));
            exam.setStudentId(rs.getInt("student_id"));
            exam.setAnswers(rs.getString("answers"));
            exam.setTakenAt(rs.getTimestamp("taken_at"));
            exam.setTotalScore(rs.getInt("total_score"));
            return exam;
        }
    }
}
