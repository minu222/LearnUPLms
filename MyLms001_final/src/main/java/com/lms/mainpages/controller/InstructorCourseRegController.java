package com.lms.mainpages.controller;

import com.lms.mainpages.entity.User;
import com.lms.mainpages.service.CourseService;
import com.lms.mainpages.web.CourseForm;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/instructor/courses")
public class InstructorCourseRegController {

    private final CourseService courseService;

    /**
     * 강의 등록 폼 (GET)
     */
    @GetMapping("/add")
    public String addCourseForm(Model model, HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/login";
        }

        model.addAttribute("instructorId", login.getUser_id()); // hidden input에 넣을 실제 강사 ID
        model.addAttribute("form", new CourseForm());
        return "courseAdd"; // Thymeleaf template 이름
    }

    /**
     * 강의 등록 처리 (POST)
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Map<String, Object> addCourse(
            HttpSession session,
            @ModelAttribute CourseForm form,
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam(name = "examFile", required = false) MultipartFile examFile,
            @RequestParam(name = "videoFile", required = false) MultipartFile videoFile
    ) throws IOException {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        int instructorId = login.getUser_id();

        // 1️⃣ 강의 생성
        long courseId = courseService.createCourse(instructorId, form, mainImage, examFile);

        // 2️⃣ VOD 동영상 저장 (선택)
        if (videoFile != null && !videoFile.isEmpty()) {
            courseService.saveVideoMaterial(courseId, videoFile);
        }

        return Map.of("courseId", courseId); // JSON 반환
    }
}