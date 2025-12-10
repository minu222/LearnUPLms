package com.lms.mainpages.controller;

import com.lms.mainpages.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 역할 기반 진입 및 레거시 경로 호환 게이트웨이
 * - INSTRUCTOR  : /myclass/teacher/classes 로 리다이렉트
 * - 그 외(학생)   : /myclass/student/courses 로 리다이렉트
 * - 레거시 지원  : /myclass/studentCourses, /myclass/teacherClasses → 정식 경로로 301/302
 */
@Controller
public class MyClassGatewayController {

    /** 역할기반 진입 (헤더가 이 경로를 바라보면 가장 확실) */
    @GetMapping("/myclass/entry")
    public String entry(HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/user/login?redirect=/myclass/entry";
        }
        return isInstructor(login)
                ? "redirect:/myclass/teacher/classes"
                : "redirect:/myclass/student/courses";
    }

    /** 레거시 경로 호환: camelCase 학생 경로 → 정식 경로 */
    @GetMapping("/myclass/studentCourses")
    public String legacyStudentCourses() {
        return "redirect:/myclass/student/courses";
    }

    /** 레거시 경로 호환: camelCase 강사 경로 → 정식 경로 */
    @GetMapping("/myclass/teacherClasses")
    public String legacyTeacherClasses() {
        return "redirect:/myclass/teacher/classes";
    }

    /** 역할명이 INSTRUCTOR 인지 확인 (대소문자 무시) */
    private boolean isInstructor(User user) {
        // case 1: getRole() == "INSTRUCTOR"
        try {
            var m = user.getClass().getMethod("getRole");
            Object v = m.invoke(user);
            if (v != null && "INSTRUCTOR".equalsIgnoreCase(String.valueOf(v))) return true;
        } catch (Exception ignored) {}

        // case 2: getUserType() == "INSTRUCTOR"
        try {
            var m = user.getClass().getMethod("getUserType");
            Object v = m.invoke(user);
            if (v != null && "INSTRUCTOR".equalsIgnoreCase(String.valueOf(v))) return true;
        } catch (Exception ignored) {}

        // case 3: getRoles() 문자열/컬렉션에 "INSTRUCTOR" 포함
        try {
            var m = user.getClass().getMethod("getRoles");
            Object v = m.invoke(user);
            if (v != null && v.toString().toUpperCase().contains("INSTRUCTOR")) return true;
        } catch (Exception ignored) {}

        return false;
    }
}
