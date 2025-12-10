package com.lms.mainpages.api;

import com.lms.mainpages.entity.User;
import com.lms.mainpages.exam.MockExamResult;
import com.lms.mainpages.exam.MockExamResultRepository;
import com.lms.mainpages.repository.EnrollmentRepository;
import com.lms.mainpages.exam.MockExamResultRepository; // 시험 점수 조회 Repository 필요
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentApiController {
    private final EnrollmentRepository repo;
    private final MockExamResultRepository examRepo;

    public EnrollmentApiController(EnrollmentRepository repo,
                                   MockExamResultRepository examRepo) {
        this.repo = repo;
        this.examRepo = examRepo;
    }

    @GetMapping("/my")
    public ResponseEntity<?> myEnrollments(HttpSession session){
        User login = (User) session.getAttribute("loginUser");
        if (login == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));

        // 1️⃣ 수강 등록 리스트 가져오기
        List<Map<String,Object>> enrollments = (List<Map<String,Object>>) repo.listByStudent(login.getUser_id());

        // 2️⃣ enrollments에 점수 포함
        List<Map<String, Object>> enriched = enrollments.stream().map(e -> {
            Integer courseId = (Integer) e.get("course_id");

            // ✅ 강의 + 학생 기준으로 시험 점수 조회
            List<MockExamResult> results = examRepo.findByStudentId(login.getUser_id())
                    .stream()
                    .filter(r -> r.getCourseTitle() != null
                            && r.getCourseTitle().trim().equalsIgnoreCase(e.get("title").toString().trim()))
                    .collect(Collectors.toList());
            int totalScore = results.isEmpty() ? 0 : results.get(results.size() - 1).getTotalScore();

            Map<String, Object> m = new HashMap<>(e);
            m.put("score", totalScore);

            System.out.println(totalScore);

            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(enriched);
    }
}
