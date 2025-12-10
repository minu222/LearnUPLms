// src/main/java/dwacademy/mylms001/controller/MyClassPageController.java
package com.lms.mainpages.controller;

import com.lms.mainpages.repository.CourseMaterialRepository;
import com.lms.mainpages.repository.CourseRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/myclass")
public class MyClassPageController {

    private final CourseRepository courseRepository;
    private final CourseMaterialRepository materialRepository;

    public MyClassPageController(CourseRepository courseRepository,
                                 CourseMaterialRepository materialRepository) {
        this.courseRepository = courseRepository;
        this.materialRepository = materialRepository;
    }

    /**
     * 예: /myclass/lectureroom?courseId=123&popup=1
     * - courses.title을 coursetitle/title로 바인딩
     * - 첫 재생 가능한 동영상 있을 경우 videoUrl/videoType/videoName 바인딩
     * - popup 파라미터가 truthy면 isPopup=true
     */
    @GetMapping("/lectureroom")
    public String lectureRoom(@RequestParam("courseId") long courseId,
                              @RequestParam(value = "popup", required = false, defaultValue = "false") String popup,
                              Model model) {

        // 강의 제목 (SSOT: courses.title)
        String title = courseRepository.findTitleById(courseId).orElse("강의실");

        model.addAttribute("courseId", courseId);
        model.addAttribute("coursetitle", title); // 템플릿의 <span th:text="${coursetitle}">
        model.addAttribute("title", title);       // 상단 제목에도 사용
        model.addAttribute("isPopup", isTruthy(popup));

        // 첫 번째 재생 가능한 동영상(예: file_type LIKE 'video/%') 조회
        materialRepository.findFirstPlayableVideo(courseId).ifPresent(v -> {
            // 파일 스트리밍 엔드포인트 예시: /files/stream?path=ABSOLUTE_PATH
            String url = "/files/stream?path=" + UriUtils.encode(v.filePath(), StandardCharsets.UTF_8);
            model.addAttribute("videoUrl", url);
            model.addAttribute("videoType", v.fileType()); // 예: video/mp4
            model.addAttribute("videoName", v.name());
        });

        return "myclass/lectureroom"; // => templates/myclass/lectureroom.html
    }

    /**
     * PathVariable 버전: /myclass/lecture/{courseId}?popup=1
     * 내부적으로 위 메서드로 위임
     */
    @GetMapping("/lecture/{courseId}")
    public String lectureRoom2(@PathVariable long courseId,
                               @RequestParam(value = "popup", required = false, defaultValue = "false") String popup,
                               Model model) {
        return lectureRoom(courseId, popup, model);
    }

    /** "1", "true", "yes", "on" 등을 true로 판단 */
    private boolean isTruthy(String v) {
        if (v == null) return false;
        String s = v.trim().toLowerCase();
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("on");
    }

}
