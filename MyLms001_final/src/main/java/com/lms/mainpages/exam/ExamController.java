package com.lms.mainpages.exam;

import com.lms.mainpages.exam.entity.CourseInfoDTO;
import com.lms.mainpages.exam.entity.ExamQuestion;
import com.lms.mainpages.repository.CourseRepository;
import com.lms.mainpages.exam.ExamQuestionRepository;
import com.lms.mainpages.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ExamController {

    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final DataSource dataSource;

    /** ✅ 시험 ID 조회 — 없으면 Optional.empty() 반환 */
    private Optional<Integer> getExamIdForCourse(String courseTitle) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT DISTINCT exam_id FROM exam_questions WHERE course_title = ? LIMIT 1")) {
            pstmt.setString(1, courseTitle);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("exam_id"));
                } else {
                    return Optional.empty(); // ❌ 예외 던지지 않고 빈 값 반환
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("시험 조회 중 DB 오류 발생", e);
        }
    }

    /** ================== 시험 페이지(GET) ================== */
    @GetMapping("/exam/start/{courseId}")
    public String examPage(
            @PathVariable("courseId") long courseId,
            HttpServletRequest request,
            Model model) {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return "redirect:/login?redirect=/exam/start/" + courseId;
        }

        int studentId = ((Long) session.getAttribute("userId")).intValue();

        // ✅ 강의 정보(제목 + 강사ID)
        CourseInfoDTO courseInfo = courseRepository.findTitleAndInstructorById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        String courseTitle = courseInfo.getTitle();
        Long instructorId = courseInfo.getInstructorId();

        // ✅ 시험 문제 조회
        List<ExamQuestion> questions = examQuestionRepository
                .findByInstructorIdAndCourseTitle(instructorId, courseTitle);

        if (questions == null || questions.isEmpty()) {
            model.addAttribute("alertMessage", "해당 강의에 등록된 시험문제가 없습니다.");
            model.addAttribute("redirectUrl", "/myclass/student/courses");
            return "myclass/alert"; // ✅ alert 전용 페이지
        }

        // ✅ 시험 ID 조회
        Optional<Integer> examIdOpt = getExamIdForCourse(courseTitle);
        if (examIdOpt.isEmpty()) {
            model.addAttribute("alertMessage", "해당 강의에 시험이 존재하지 않습니다.");
            model.addAttribute("redirectUrl", "/myclass/student/courses");
            return "myclass/examspage"; // alert 표시 후 이동 처리
        }

        int examId = examIdOpt.get();

        // ✅ 모델 데이터 전달
        model.addAttribute("examId", examId);
        model.addAttribute("questions", questions);
        model.addAttribute("studentId", studentId);
        model.addAttribute("courseId", courseId);
        model.addAttribute("courseTitle", courseTitle);
        model.addAttribute("instructorId", instructorId);

        return "myclass/examspage";
    }
    /** ================== 시험 제출(POST) ================== */
    @PostMapping("/submit")
    public String submitExam(HttpServletRequest request, HttpSession session) {
        int studentId = ((Long) session.getAttribute("userId")).intValue();

        // examId, courseTitle, questionCount 유효성 검사
        String examIdStr = request.getParameter("examId");
        String courseTitle = request.getParameter("courseTitle");
        String questionCountStr = request.getParameter("questionCount");

        if (examIdStr == null || courseTitle == null || questionCountStr == null) {
            throw new IllegalArgumentException("시험 데이터가 올바르지 않습니다.");
        }

        int examId = Integer.parseInt(examIdStr);
        int questionCount = Integer.parseInt(questionCountStr);

        // ✅ 답안 수집
        Map<String, String> answersMap = new HashMap<>();
        for (int i = 0; i < questionCount; i++) {
            String answer = request.getParameter("q" + i);
            answersMap.put("q" + i, answer != null ? answer : "");
        }
        String answersStr = answersMap.toString();

        // ✅ 점수 계산
        int totalScore = 0;
        List<ExamQuestion> questions = examQuestionRepository.findByCourseTitle(courseTitle);
        for (int i = 0; i < questions.size(); i++) {
            ExamQuestion question = questions.get(i);
            String correctAnswer = question.getAnswer();
            String userAnswer = answersMap.getOrDefault("q" + i, "");
            if (correctAnswer.equals(userAnswer)) {
                totalScore += question.getScore();
            }
        }

        // ✅ 결과 DB 저장
        String sql = "INSERT INTO mock_exam_results (exam_id, course_title, student_id, answers, total_score) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            pstmt.setString(2, courseTitle);
            pstmt.setInt(3, studentId);
            pstmt.setString(4, answersStr);
            pstmt.setInt(5, totalScore);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("시험 결과 저장 중 오류 발생", e);
        }

        return "redirect:/myclass/student/exams";
    }
}
