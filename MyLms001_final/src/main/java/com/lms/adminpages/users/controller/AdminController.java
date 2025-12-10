package com.lms.adminpages.users.controller;

import com.lms.adminpages.users.dao.UserDao;
import com.lms.adminpages.users.entity.User;
import com.lms.adminpages.users.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final UserDao userDao;

    public AdminController(UserService userService, UserDao userDao) {
        this.userService = userService;
        this.userDao = userDao;
    }

    @GetMapping("/instructors")
    public String instructorList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String keywordType,
            @RequestParam(required = false) String keyword,
            Model model) {

        // 필터 적용한 강사 리스트 조회
        List<User> instructors = userService.getInstructors(status, department, keywordType, keyword);

        // 모델에 추가
        model.addAttribute("instructors", instructors);
        model.addAttribute("statusFilter", status);
        model.addAttribute("departmentFilter", department);
        model.addAttribute("keywordType", keywordType);
        model.addAttribute("keyword", keyword);

        // templates/adminpages/instructor-info/index.html 경로
        return "adminpages/instructor-info/index";
    }

/*

    @PostMapping("/instructors/delete")
    public String deleteInstructors(@RequestParam("ids") int[] ids) {
        userService.deleteUsers(ids);
        return "redirect:/admin/instructors";
    }
*/


    @GetMapping("/students")
    public String studentList(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String department,
                              @RequestParam(required = false) String keywordType,
                              @RequestParam(required = false) String keyword,
                              Model model) {

        List<User> students = userService.getStudents(status, department, keywordType, keyword);

        model.addAttribute("students", students);
        model.addAttribute("statusFilter", status);         // 상태 필터 선택 유지
        model.addAttribute("departmentFilter", department); // 소속 필터 선택 유지
        model.addAttribute("keywordType", keywordType);     // 검색 타입 선택 유지
        model.addAttribute("keyword", keyword);             // 검색어 유지

        return "adminpages/student-info/index"; // templates 폴더 내 강사 정보 페이지
    }

//    @PostMapping("/students/delete")
//    public String deleteStudents(@RequestParam("ids") int[] ids) {
//        userService.deleteUsers(ids);
//        return "redirect:/admin/students";
//    }


    // 멤버 상세페이지

    @GetMapping("/member-details")
    public String listMembers(Model model) {
        List<User> members = userDao.findAll();
        model.addAttribute("members", members);
        return "adminpages/member-details/index"; // ← 위에 주신 HTML 파일
    }

    @GetMapping("/members")
    public String listMembers(
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model
    ) {
        model.addAttribute("members", userService.getMemberList(group, type, keyword));
        model.addAttribute("group", group);
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        return "adminpages/member-details/index";  // Thymeleaf 목록 페이지
    }

    @GetMapping("/members/{userId}")
    public String detailMember(@PathVariable("userId") Integer userId, Model model) {
        User selectedMember = userDao.findById(userId);
        model.addAttribute("member", selectedMember);
        return "adminpages/member-details/detail"; // 위에 만든 HTML
    }


    @PostMapping("/member-details/delete")
    public String deleteSelected(@RequestParam(value = "selectedIds", required = false) int[] selectedIds) {
        if (selectedIds != null && selectedIds.length > 0) {
            userService.deleteMembers(selectedIds);
        }
        return "redirect:/admin/member-details";
    }

    //정보 휴지통

    @GetMapping("/info-trash")
    public String viewTrash(Model model) {
        model.addAttribute("trashList", userService.getTrashList());
        return "adminpages/info-trash/index";
    }


    @PostMapping("/member-details/restore")
    public String restoreUsers(@RequestParam("ids") List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            userService.restoreUsers(ids);
        }
        return "redirect:/admin/info-trash";
    }
}