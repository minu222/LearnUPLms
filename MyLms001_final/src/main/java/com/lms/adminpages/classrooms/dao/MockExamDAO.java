package com.lms.adminpages.classrooms.dao;

import com.lms.adminpages.classrooms.entity.MockExam;
import com.lms.adminpages.users.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MockExamDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 전체 조회 (JOIN)
    public List<MockExam> findAll() {
        String sql = """
            SELECT eq.exam_id, eq.instructor_id, eq.course_title, eq.question, eq.option1, eq.option2, eq.option3, eq.option4, eq.answer, eq.score, eq.taken_at,
                   u.nickname AS instructorName
            FROM exam_questions eq
            LEFT JOIN users u ON eq.instructor_id = u.user_id
            ORDER BY eq.exam_id DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> MockExam.builder()
                .examId(rs.getInt("exam_id"))
                .instructorId(rs.getInt("instructor_id"))
                .courseTitle(rs.getString("course_title"))
                .question(rs.getString("question"))
                .option1(rs.getString("option1"))
                .option2(rs.getString("option2"))
                .option3(rs.getString("option3"))
                .option4(rs.getString("option4"))
                .answer(rs.getString("answer"))
                .score(rs.getInt("score"))
                .takenAt(rs.getTimestamp("taken_at"))
                .instructorName(rs.getString("instructorName")) // 화면용
                .build()
        );
    }

    public List<User> findAllInstructors() {
        String sql = "SELECT user_id, nickname FROM users WHERE role='instructor'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> User.builder()
                .user_id(rs.getInt("user_id"))
                .nickname(rs.getString("nickname"))
                .build()
        );
    }

    public List<MockExam> findByInstructorName(String instructorName) {
        String sql = """
        SELECT eq.exam_id, eq.instructor_id, eq.course_title, eq.question, eq.option1, eq.option2, eq.option3, eq.option4, eq.answer, eq.score, eq.taken_at,
               u.nickname AS instructorName
        FROM exam_questions eq
        LEFT JOIN users u ON eq.instructor_id = u.user_id
        WHERE u.nickname LIKE ?
        ORDER BY eq.exam_id DESC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> MockExam.builder()
                .examId(rs.getInt("exam_id"))
                .instructorId(rs.getInt("instructor_id"))
                .courseTitle(rs.getString("course_title"))
                .question(rs.getString("question"))
                .option1(rs.getString("option1"))
                .option2(rs.getString("option2"))
                .option3(rs.getString("option3"))
                .option4(rs.getString("option4"))
                .answer(rs.getString("answer"))
                .score(rs.getInt("score"))
                .takenAt(rs.getTimestamp("taken_at"))
                .instructorName(rs.getString("instructorName"))
                        .build(),
                "%" + instructorName + "%");
    }


    public MockExam findById(int examId) {
        String sql = """
            SELECT eq.exam_id, eq.instructor_id, eq.course_title, eq.question, eq.option1, eq.option2, eq.option3, eq.option4, eq.answer, eq.score, eq.taken_at,
               u.nickname AS instructorName
            FROM exam_questions eq
            LEFT JOIN users u ON eq.instructor_id = u.user_id
            WHERE eq.exam_id = ?
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> MockExam.builder()
                        .examId(rs.getInt("exam_id"))
                        .instructorId(rs.getInt("instructor_id"))
                        .courseTitle(rs.getString("course_title"))
                        .question(rs.getString("question"))
                        .option1(rs.getString("option1"))
                        .option2(rs.getString("option2"))
                        .option3(rs.getString("option3"))
                        .option4(rs.getString("option4"))
                        .answer(rs.getString("answer"))
                        .score(rs.getInt("score"))
                        .takenAt(rs.getTimestamp("taken_at"))
                        .instructorName(rs.getString("instructorName"))
                        .build()
                , examId);
    }
    private MockExam mapRowToMockExam(ResultSet rs) throws SQLException {
        return MockExam.builder()
                .examId(rs.getInt("exam_id"))
                .instructorId(rs.getInt("instructor_id"))
                .courseTitle(rs.getString("course_title"))
                .question(rs.getString("question"))
                .option1(rs.getString("option1"))
                .option2(rs.getString("option2"))
                .option3(rs.getString("option3"))
                .option4(rs.getString("option4"))
                .answer(rs.getString("answer"))
                .score(rs.getInt("score"))
                .takenAt(rs.getTimestamp("taken_at"))
                .build();
    }

    // 등록
    public void save(MockExam exam) {
        String sql = """
            INSERT INTO exam_questions (instructor_id, course_title, question, option1, option2, option3, option4, answer, score)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                exam.getInstructorId(),
                exam.getCourseTitle(),
                exam.getQuestion(),
                exam.getOption1(),
                exam.getOption2(),
                exam.getOption3(),
                exam.getOption4(),
                exam.getAnswer(),
                exam.getScore()
        );
    }

    // 수정
    public void update(MockExam exam) {
        String sql = """
            UPDATE exam_questions
            SET instructor_id=?, course_title=?, question=?, option1=?, option2=?, option3=?, option4=?, answer=?, score=?
            WHERE exam_id=?
        """;
        jdbcTemplate.update(sql,
                exam.getInstructorId(),
                exam.getCourseTitle(),
                exam.getQuestion(),
                exam.getOption1(),
                exam.getOption2(),
                exam.getOption3(),
                exam.getOption4(),
                exam.getAnswer(),
                exam.getScore(),
                exam.getExamId()
        );
    }

    // 삭제
    public void delete(int examId) {
        String sql = "DELETE FROM exam_questions WHERE exam_id=?";
        jdbcTemplate.update(sql, examId);
    }
}