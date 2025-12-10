package com.lms.adminpages.classrooms.controller;

import com.lms.adminpages.classrooms.entity.*;
import com.lms.adminpages.classrooms.service.ClassroomService;
import com.lms.adminpages.classrooms.service.CourseMaterialService;
import com.lms.adminpages.classrooms.service.MockExamService;
import com.lms.adminpages.users.dao.UserDao;
import com.lms.adminpages.users.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.management.relation.Role;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class ClassroomController {

    private final ClassroomService classroomService;
    private final UserDao userDao;
    private final JdbcTemplate jdbcTemplate;
    private final CourseMaterialService courseMaterialService;
    private final MockExamService mockExamService;


    public ClassroomController(ClassroomService classroomService, UserDao userDao, JdbcTemplate jdbcTemplate, CourseMaterialService courseMaterialService
    , MockExamService mockExamService) {
        this.classroomService = classroomService;
        this.userDao = userDao;
        this.jdbcTemplate = jdbcTemplate;
        this.courseMaterialService = courseMaterialService;
        this.mockExamService = mockExamService;
    }

    //-----------------------------------강의실 등록
    @GetMapping("/classrooms-registration")
    public String showRegisterForm(Model model) {
        model.addAttribute("classroom", new Classroom());
        List<User> instructors = classroomService.findAllInstructors();
        model.addAttribute("instructors", instructors);
        List<String> categories = classroomService.findAllCategories();
        model.addAttribute("categories", categories);


        return "adminpages/classroom-registration/index";
    }

    @PostMapping("/classrooms-registration")
    public String registerClassroom(
            @ModelAttribute Classroom classroom,
            RedirectAttributes ra)  throws IOException {

        // 강사 체크
        Integer instructorId = classroom.getInstructorId();
        if (instructorId == null) {
            ra.addFlashAttribute("errorMessage", "강사 ID를 입력해주세요.");
            return "redirect:/admin/classrooms-registration";
        }

        User instructor = classroomService.findUserById(instructorId);
        if (instructor == null || "deleted".equalsIgnoreCase(instructor.getStatus().name())) {
            ra.addFlashAttribute("errorMessage", "유효하지 않은 강사입니다.");
            return "redirect:/admin/classrooms-registration";
        }

        try {
            // 1️⃣ 강의실 저장
            classroomService.save(classroom);

            // 2️⃣ 수업자료 저장
            if (classroom.getMaterialFiles() != null) {
                for (int i = 0; i < classroom.getMaterialFiles().size(); i++) {
                    MultipartFile file = classroom.getMaterialFiles().get(i);
                    if (!file.isEmpty()) {
                        String fileName = file.getOriginalFilename().replaceAll("\\s+", "_");
                        String uploadDir = "C:/lms_uploads/course_materials/" + classroom.getTitle();
                        File folder = new File(uploadDir);
                        if (!folder.exists()) folder.mkdirs();
                        File dest = new File(folder, fileName);
                        file.transferTo(dest);

                        CourseMaterial material = new CourseMaterial();
                        material.setCourseId(classroom.getClassroomId());
                        material.setCourseTitle(classroom.getTitle());
                        material.setName(fileName);
                        material.setFilePath(dest.getAbsolutePath());
                        material.setFileType(Files.probeContentType(dest.toPath()));

                        // hasExam 안전 처리
                        boolean hasExam = false;
                        if (classroom.getHasExam() != null && !classroom.getHasExam().isEmpty()) {
                            hasExam = classroom.getHasExam().get(i); // i번째 자료의 hasExam
                        }
                        material.setHasExam(hasExam);
                        material.setHasReplay(true);

                        courseMaterialService.saveMaterial(material);
                    }
                }
            }

            // 3️⃣ 시험자료 저장
            if (classroom.getExams() != null) {
                for (MockExam exam : classroom.getExams()) {
                    exam.setInstructorId(classroom.getInstructorId());
                    exam.setCourseTitle(classroom.getTitle());
                    mockExamService.saveExam(exam);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "파일 업로드 중 오류가 발생했습니다.");
            return "redirect:/admin/classrooms-registration";
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "강의실 등록 중 오류가 발생했습니다.");
            return "redirect:/admin/classrooms-registration";
        }

        ra.addFlashAttribute("message", "강의실, 수업자료, 시험자료가 등록되었습니다.");
        return "redirect:/admin/classrooms-registration";
    }
//    --------------


    //---------------------------강의실 목록-----------------
    @GetMapping("/classrooms-list")
    public String listClassrooms(
            @ModelAttribute("filter") CourseFilter filter,
            Model model
    ) {
        List<String> categories = classroomService.findAllCategories();
        model.addAttribute("categories", categories);

        List<Classroom> courses;

        if ((filter.getCategory() == null || filter.getCategory().isEmpty()) &&
                (filter.getStatus() == null || filter.getStatus().isEmpty()) &&
                (filter.getKeyword() == null || filter.getKeyword().isEmpty())) {
            courses = classroomService.findAll();
        } else {
            courses = classroomService.findByFilterFromDB(filter);
        }

        model.addAttribute("courses", courses);

        return "adminpages/classroom-list/index";
    }

    // 상태 업데이트
    @PostMapping("/classrooms/update-status-bulk")
    public String updateStatusBulk(
            @RequestParam Map<String, String> statusMap,
            @RequestParam(value = "saveSingle", required = false) Integer singleId,
            RedirectAttributes ra
    ) {
        Map<Integer, String> statusMapInt;

        if (singleId != null) {
            // 한 행만 저장
            String status = statusMap.get("statusMap[" + singleId + "]");
            statusMapInt = Map.of(singleId, status);
        } else {
            // 전체 저장
            statusMapInt = statusMap.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("statusMap["))
                    .collect(Collectors.toMap(
                            e -> Integer.parseInt(e.getKey().replaceAll("statusMap\\[|\\]", "")),
                            Map.Entry::getValue
                    ));
        }

        classroomService.updateStatus(statusMapInt);

        ra.addFlashAttribute("successMessage", "강의실 상태가 업데이트되었습니다.");
        return "redirect:/admin/classrooms-list";
    }

    //선택 삭제
    @PostMapping("/classrooms/delete-selected")
    public String deleteSelected(
            @RequestParam(value = "ids", required = false) List<Integer> ids,
            RedirectAttributes ra
    ) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "삭제할 강의실을 선택해주세요.");
            return "redirect:/admin/classrooms-list";
        }

        classroomService.deleteByIds(ids);
        ra.addFlashAttribute("successMessage", "선택한 강의실이 삭제되었습니다.");
        return "redirect:/admin/classrooms-list";
    }

//    -----------------------------------------


 /*   //출석관리
    @GetMapping("/attendance-management")
    public String attendanceClassroom() {
        return "adminpages/attendance-management/index";
    }
*/
}
