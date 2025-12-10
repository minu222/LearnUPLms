package com.lms.adminpages.classrooms.controller;

import com.lms.adminpages.classrooms.entity.Classroom;
import com.lms.adminpages.classrooms.entity.StudentDto;
import com.lms.adminpages.classrooms.service.ClassroomService;
import com.lms.adminpages.users.dao.UserDao;
import com.lms.adminpages.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class ClassroomStudentController {


    private final ClassroomService classroomService;
    private final UserDao userDao;
    private final JdbcTemplate jdbcTemplate;

    public ClassroomStudentController(ClassroomService classroomService, UserDao userDao, JdbcTemplate jdbcTemplate) {
        this.classroomService = classroomService;
        this.userDao = userDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/classroom-student-info")
    public String StudentInClassroom(Model model) {
        // 처음 페이지 접속 시 전체 강의실 목록 가져오기
        List<Classroom> classrooms = classroomService.searchClassrooms(""); // 빈 문자열이면 전체
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("keyword", ""); // 검색창 기본값 {
        return "adminpages/classroom-student-info/index";
    }



    // 🔎 강의실 검색
    @GetMapping("/classrooms/search")
    public String searchClassrooms(@RequestParam(required = false) String keyword, Model model) {
        List<Classroom> classrooms = classroomService.searchClassrooms(keyword == null ? "" : keyword);
        model.addAttribute("classrooms", classrooms);
        model.addAttribute("keyword", keyword);
        return "adminpages/classroom-student-info/index";
    }

    // 👨‍🎓 특정 강의실 학생 목록
    @GetMapping("/classrooms/students/{courseId}")
    public String getStudents(@PathVariable int courseId, Model model,
                              @RequestParam(required = false) String keyword) {
        List<Classroom> classrooms = classroomService.searchClassrooms(keyword == null ? "" : keyword);
        List<StudentDto> students = classroomService.getStudentsByCourseId(courseId);

        model.addAttribute("classrooms", classrooms);
        model.addAttribute("students", students);
        model.addAttribute("selectedCourseId", courseId);
        return "adminpages/classroom-student-info/index";
    }
}
