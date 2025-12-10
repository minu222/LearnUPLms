package com.lms.mainpages.controller;

import com.lms.mainpages.repository.CourseMaterialRepository;
import com.lms.mainpages.repository.CourseRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LectureRoomController {

    private final CourseMaterialRepository materialRepo;
    private final CourseRepository courseRepo;

    public LectureRoomController(CourseMaterialRepository materialRepo,
                                 CourseRepository courseRepo) {
        this.materialRepo = materialRepo;
        this.courseRepo = courseRepo;
    }
    /**
     * 팝업 전용 강의실
     * 예) /myclass/lectureroom?courseId=49&popup=1
     *  - popup=1 조건으로 매핑을 제한하여 MyClassPageController 와 충돌을 피함
     */
    @GetMapping(value = "/myclass/lectureroom", params = "popup=1")
    public String lectureRoomPopup(@RequestParam("courseId") long courseId,
                                   Model model) {

        var opt = materialRepo.findFirstVideoByCourseId(courseId);

        String videoUrl  = null;
        String videoType = null;
        String videoName = null;

        String courseTitle = courseRepo.findTitleById(courseId).orElse("제목 없음");
        model.addAttribute("coursetitle", courseTitle);

        if (opt.isPresent()) {
            var m = opt.get();
            videoUrl  = "/media/course/" + m.materialId(); // 스트리밍 엔드포인트
            videoType = (m.fileType() == null || m.fileType().isBlank()) ? "video/mp4" : m.fileType();
            videoName = m.name();
        }

        model.addAttribute("courseId", courseId);
        model.addAttribute("videoUrl", videoUrl);
        model.addAttribute("videoType", videoType);
        model.addAttribute("videoName", videoName);
        model.addAttribute("isPopup", true);

        // 팝업은 레이아웃 없이 템플릿만 바로 렌더
        return "myclass/lectureroom";
    }
}
