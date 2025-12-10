package com.lms.mainpages.controller;

import com.lms.mainpages.exam.MockExamResult;
import com.lms.mainpages.exam.MockExamResultRepository;
import com.lms.mainpages.web.CourseCreateForm; // ✅ 추가: 폼 객체 임포트

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/myclass")
@RequiredArgsConstructor
public class MyClassController {

    /**
     * 나의 강의실 공통 모델 설정 메서드
     * @param model Spring Model 객체
     * @param fragment Thymeleaf fragment 경로
     * @param title 페이지 제목
     * @param userRole 사용자 역할 (guest, student, teacher)
     * @return layout 템플릿명
     */
    private String setupMyClassModel(Model model, String fragment, String title, String userRole) {
        model.addAttribute("bodyFragment", fragment);
        model.addAttribute("title", title);
        model.addAttribute("showSidebar", true); // 사이드바 표시
        model.addAttribute("userRole", userRole);
        model.addAttribute("activeCategory", "myclass");
        return "layout";
    }

    /** 나의 강의실 메인 페이지 - /myclass */
    @GetMapping
    public String myClassPage(Model model) {
        return setupMyClassModel(model,
                "myclass/myclass :: content",
                "MyLms",
                "null"
        );
    }

    /** 학생 - 수강목록 - /myclass/student/courses */
    @GetMapping("/student/courses")
    public String studentCourses(Model model) {
        return setupMyClassModel(model,
                "myclass/studentCourses :: content",
                "MyLms",
                "student"
        );
    }

    /** 학생 - 시험목록 - /myclass/student/exams */

    private final MockExamResultRepository mockExamResultRepository;

    @GetMapping("/student/exams")
    public String studentExams(HttpSession session, Model model) {
        // 로그인 체크
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        // MockExamResultRepository를 주입받아 DB 조회
        List<MockExamResult> exams = mockExamResultRepository.findByStudentId(userId.intValue());

        // 최신일 기준 내림차순 정렬
        exams.sort((e1, e2) -> e2.getTakenAt().compareTo(e1.getTakenAt()));

        model.addAttribute("exams", exams);

        // 공통 레이아웃 + fragment 설정
        return setupMyClassModel(model,
                "myclass/studentExams :: content",
                "MyLms",
                "student"
        );
    }


    /** 강사 - 강의실목록 - /myclass/teacher/classes */
    @GetMapping("/teacher/classes")
    public String teacherClasses(Model model) {
        return setupMyClassModel(model,
                "myclass/teacherClasses :: content",
                "MyLms",
                "instructor"
        );
    }

    /** 강사 - 강의실등록 - /myclass/teacher/register */
    @GetMapping("/teacher/register")
    public String teacherRegisterClass(Model model) {
        // ✅ 최소 변경: 폼 객체가 없으면 기본 폼을 넣어줌 (Thymeleaf #fields 사용 시 필수)
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CourseCreateForm());
        }
        return setupMyClassModel(model,
                "myclass/teacherRegisterClass :: content",
                "MyLms",
                "instructor"
        );
    }

    /** 공통 - 결제내역 - /myclass/payment */
    @GetMapping("/payment")
    public String paymentHistory(Model model) {
        return setupMyClassModel(model,
                "myclass/paymentHistory :: content",
                "MyLms",
                "student"
        );
    }

    /** 공통 - 나의게시판 - /myclass/myBoard */
    @GetMapping("/myBoard")
    public String myBoard(Model model) {
        return setupMyClassModel(model,
                "myclass/myBoard :: content",
                "MyLms",
                "student"
        );
    }

    /** 공통 - 이메일/쪽지 - /myclass/myMessage */
    @GetMapping("/myMessage")
    public String myMessage(Model model) {
        return setupMyClassModel(model,
                "myclass/myMessage :: content",
                "MyLms",
                "student"
        );
    }

    /** 공통 - 회원정보수정 - /myclass/myInfo */
    @GetMapping("/myInfo")
    public String myInfo(Model model) {
        return setupMyClassModel(model,
                "myclass/myInfo :: content",
                "MyLms",
                "student"
        );
    }
}