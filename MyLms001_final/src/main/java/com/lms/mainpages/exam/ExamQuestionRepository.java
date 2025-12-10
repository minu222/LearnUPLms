package com.lms.mainpages.exam;

import com.lms.mainpages.exam.entity.ExamQuestion;
import com.lms.mainpages.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ExamQuestionRepository {

    private final DataSource dataSource;

    @Autowired
    public ExamQuestionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ExamQuestion> findByInstructorIdAndCourseTitle(Long instructorId, String courseTitle) {
        List<ExamQuestion> questions = new ArrayList<>();
        String sql = "SELECT * FROM exam_questions WHERE instructor_id = ? AND course_title = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Math.toIntExact(instructorId));
            pstmt.setString(2, courseTitle);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ExamQuestion q = new ExamQuestion();
                q.setExamId(rs.getInt("exam_id"));
                q.setInstructorId(rs.getInt("instructor_id"));
                q.setCourseTitle(rs.getString("course_title"));
                q.setQuestion(rs.getString("question"));
                q.setOption1(rs.getString("option1"));
                q.setOption2(rs.getString("option2"));
                q.setOption3(rs.getString("option3"));
                q.setOption4(rs.getString("option4"));
                q.setAnswer(rs.getString("answer"));
                q.setScore(rs.getInt("score"));

                questions.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return questions;
    }

    public List<ExamQuestion> findAll() {
        List<ExamQuestion> questions = new ArrayList<>();
        String sql = "SELECT * FROM exam_questions";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ExamQuestion q = new ExamQuestion();
                q.setExamId(rs.getInt("exam_id"));
                q.setInstructorId(rs.getInt("instructor_id"));
                q.setCourseTitle(rs.getString("course_title"));
                q.setQuestion(rs.getString("question"));
                q.setOption1(rs.getString("option1"));
                q.setOption2(rs.getString("option2"));
                q.setOption3(rs.getString("option3"));
                q.setOption4(rs.getString("option4"));
                q.setAnswer(rs.getString("answer"));
                q.setScore(rs.getInt("score"));

                questions.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return questions;
    }


    public List<ExamQuestion> findByCourseTitle(String courseTitle) {
        List<ExamQuestion> questions = new ArrayList<>();
        String sql = "SELECT exam_id, instructor_id, course_title, question, option1, option2, option3, option4, answer, score " +
                "FROM exam_questions WHERE course_title = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseTitle);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ExamQuestion question = new ExamQuestion();
                question.setExamId(rs.getInt("exam_id"));
                question.setInstructorId(rs.getInt("instructor_id"));
                question.setCourseTitle(rs.getString("course_title"));
                question.setQuestion(rs.getString("question"));
                question.setOption1(rs.getString("option1"));
                question.setOption2(rs.getString("option2"));
                question.setOption3(rs.getString("option3"));
                question.setOption4(rs.getString("option4"));
                question.setAnswer(rs.getString("answer"));
                question.setScore(rs.getInt("score"));
                questions.add(question);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return questions;
    }

}
