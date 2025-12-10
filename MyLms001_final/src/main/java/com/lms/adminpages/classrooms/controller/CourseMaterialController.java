package com.lms.adminpages.classrooms.controller;

import com.lms.adminpages.classrooms.dao.ClassroomDAO;
import com.lms.adminpages.classrooms.entity.Classroom;
import com.lms.adminpages.classrooms.entity.CourseMaterial;
import com.lms.adminpages.classrooms.service.ClassroomService;
import com.lms.adminpages.classrooms.service.CourseMaterialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Controller
@RequestMapping("/admin")
public class CourseMaterialController {

    private final CourseMaterialService courseMaterialService;

    // 파일 업로드 기본 경로 (실제 경로는 환경에 맞게 수정)
    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ClassroomDAO classroomDAO;

    public CourseMaterialController(CourseMaterialService courseMaterialService, ClassroomDAO classroomDAO) {
        this.courseMaterialService = courseMaterialService;
        this.classroomDAO = classroomDAO;
    }

    @GetMapping("/course-materials-management")
    public String listCourseMaterials(@RequestParam(value = "q", required = false) String keyword, Model model) {
        List<CourseMaterial> materials = courseMaterialService.searchMaterials(keyword);
        model.addAttribute("materials", materials);
        return "adminpages/course-materials-management/index"; // Thymeleaf 템플릿 경로
    }


    @GetMapping("/course-materials/{id}/view")
    public String viewCourseMaterial(@PathVariable("id") int materialId, Model model) {
        CourseMaterial material = courseMaterialService.getMaterialById(materialId);
        model.addAttribute("material", material);
        return "adminpages/course-materials-management/view";  // 상세보기 페이지
    }


//    /** 업로드 폼 이동 */
//    @GetMapping("/course-materials/upload")
//    public String uploadForm(Model model) {
//        // 과정 리스트를 내려주어 select 옵션에 사용
//        List<Classroom> classrooms = classroomDAO.findAll();
//        model.addAttribute("courses", classrooms);
//        return "adminpages/course-materials-management/upload";
//    }
//
//    /** 자료 업로드 처리 */
//    @PostMapping("/course-materials/upload")
//    public String upload(@RequestParam("classroomName") String title,
//                         @RequestParam("file") MultipartFile file,
//                         @RequestParam(value = "hasExam", required = false) Boolean hasExam,
//                         @RequestParam(value = "hasReplay", required = false) Boolean hasReplay,
//                         RedirectAttributes ra) throws IOException {
//
//        boolean exam = hasExam != null && hasExam;
//        boolean replay = hasReplay == null || hasReplay;
//
//        Classroom classroom = classroomDAO.findByName(title);
//        if (classroom == null) {
//            ra.addFlashAttribute("error", "해당 강의실을 찾을 수 없습니다.");
//            return "redirect:/admin/course-materials/upload";
//        }
//        Integer classroomId = classroom.getClassroomId();
//
//        // 1. 디렉토리 생성
//        File dir = new File(uploadDir);
//        if (!dir.exists()) dir.mkdirs();
//
//        // 2. 파일 저장
//        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename(); // 충돌 방지
//        File savedFile = new File(uploadDir, fileName);
//        file.transferTo(savedFile);
//
//        // 3. DB 저장
//        CourseMaterial material = CourseMaterial.builder()
//                .courseId(classroomId)
//                .name(file.getOriginalFilename()) // 원래 파일명 표시
//                .filePath(savedFile.getAbsolutePath())
//                .fileType(file.getContentType())
//                .hasExam(exam)
//                .hasReplay(replay)
//                .build();
//
//
//        courseMaterialService.saveMaterial(material);
//        ra.addFlashAttribute("message", "자료가 업로드되었습니다.");
//
//        return "redirect:/admin/course-materials/upload";
//    }
    /** 자료 다운로드 */
    @GetMapping("/course-materials/{material_id}/download")
    public ResponseEntity<Resource> download(@PathVariable Integer material_id) throws IOException {
        CourseMaterial material = courseMaterialService.getMaterial(material_id);

        if (material == null) return ResponseEntity.notFound().build();

        File uploadDir = new File("C:\\test\\webDev\\project\\MyLms001_final\\uploads\\course-materials");


        System.out.println("경로문제"+material.getFilePath());

        File file = new File(material.getFilePath()); // DB에는 파일명만 저장

        System.out.println("DB문제"+file);

        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        String encodedFileName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.parseMediaType(material.getFileType()))
                .body(resource);
    }




    /** 자료 삭제 */
    @PostMapping("/course-materials/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        courseMaterialService.deleteMaterial(id);
        ra.addFlashAttribute("message", "자료가 삭제되었습니다.");
        return "redirect:/admin/course-materials-management";
    }
}
