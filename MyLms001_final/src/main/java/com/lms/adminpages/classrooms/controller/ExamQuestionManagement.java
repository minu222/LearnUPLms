package com.lms.adminpages.classrooms.controller;

import com.lms.adminpages.classrooms.entity.MockExam;
import com.lms.adminpages.classrooms.service.MockExamService;
import com.lms.adminpages.users.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class ExamQuestionManagement {



    @Autowired
    private MockExamService mockExamService;

    // 목록
    @GetMapping("/mock-exams")
    public String listExams(@RequestParam(required = false) String instructorName, Model model) {
        List<MockExam> exams;
        if (instructorName != null) {
            exams = mockExamService.findByInstructorName(instructorName);
        } else {
            exams = mockExamService.getAllExams();
        }
        model.addAttribute("exams", exams);
        List<User> instructors = mockExamService.findAllInstructors();
        model.addAttribute("instructors", instructors);
        return "adminpages/exam-questions-management/index";
    }


    // 등록 폼
    @GetMapping("/mock-exams/new")
    public String showCreateForm(Model model) {
        model.addAttribute("mockExam", new MockExam());
        List<User> instructors = mockExamService.findAllInstructors();

        System.out.println(model + "model");
        System.out.println(instructors);

        model.addAttribute("instructors", instructors);
        return "adminpages/exam-questions-management/form";
    }

    // 등록 처리
    @PostMapping("/mock-exams/save")
    public String saveOrUpdateExam(@ModelAttribute MockExam exam, RedirectAttributes ra) {
        if (exam.getExamId() == null) {
            mockExamService.saveExam(exam);
            ra.addFlashAttribute("message", "시험 문제가 등록되었습니다.");
        } else {
            mockExamService.updateExam(exam);
            ra.addFlashAttribute("message", "시험 문제가 수정되었습니다.");
        }
        // ❌ 잘못된 URL
        // return "redirect:/admin/exam-questions-management";

        // ✅ 올바른 URL
        return "redirect:/admin/mock-exams";
    }
    // 수정 폼
    @GetMapping("/mock-exams/{id}/edit")
    public String editExamForm(@PathVariable int id, Model model) {
        MockExam mockExam = mockExamService.getExamById(id);
        if (mockExam == null) {
            // 없으면 목록으로 리다이렉트
            return "redirect:/admin/mock-exams";
        }
        model.addAttribute("mockExam", mockExam);

        List<User> instructors = mockExamService.findAllInstructors();
        model.addAttribute("instructors", instructors);

        System.out.println("시험문제 불러오기");
        return "adminpages/exam-questions-management/form";
    }


    @PostMapping("/mock-exams/{id}/edit")
    public String updateExam(@PathVariable int id, @ModelAttribute MockExam mockExam, RedirectAttributes ra) {
        mockExam.setExamId(id);
        mockExamService.updateExam(mockExam);
        ra.addFlashAttribute("message", "시험 문제가 수정되었습니다.");
        return "redirect:/admin/mock-exams"; // 수정 후 목록으로
    }

    // 삭제
    @PostMapping("/mock-exams/{id}/delete")
    public String deleteExam(@PathVariable("id") int id, RedirectAttributes ra) {
        mockExamService.deleteExam(id);
        ra.addFlashAttribute("message", "시험 문제가 삭제되었습니다.");
        return "redirect:/admin/mock-exams";
    }
}

