package com.lms.mainpages.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequestMapping("/course")
public class CourseController {

    private String setupModel(Model model, String fragment, String title) {
        model.addAttribute("bodyFragment", fragment);
        model.addAttribute("title", title);
        model.addAttribute("activeCategory", "course");
        model.addAttribute("userRole", "guest");
        model.addAttribute("showSidebar", true);
        return "layout";
    }

    @GetMapping
    public String coursePage(Model model) {
        return setupModel(model, "course/course :: content", "MyLms");
    }

    @GetMapping("/lecture")
    public String courseLecture(Model model) {
        return setupModel(model, "course/courseLecture :: content", "MyLms");
    }

    /* ===== 소개(이과/문과/예체능) 동적 1개 라우트 ===== */

    private static final Map<String, String> TRACK_FRAGMENT = Map.of(
            "science", "course/courseIntroScience :: content",
            "liberal", "course/courseIntroLiberal :: content",
            "arts",    "course/courseIntroArts :: content"
    );

    private static final Map<String, String> TRACK_TITLE = Map.of(
            "science", "MyLms",
            "liberal", "MyLms",
            "arts",    "MyLms"
    );

    /** /course/introduction/{track} */
    @GetMapping("/introduction/{track}")
    public String courseIntroDynamic(@PathVariable String track, Model model) {
        String fragment = TRACK_FRAGMENT.get(track);
        if (fragment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown track: " + track);
        }
        model.addAttribute("track", track);
        return setupModel(model, fragment, TRACK_TITLE.get(track));
    }

    // ❌ 아래 세 메서드는 삭제하세요 (자기 자신으로 redirect → 무한 리다이렉트 발생)
    // @GetMapping("/introduction/science")  -> redirect:/course/introduction/science
    // @GetMapping("/introduction/liberal")  -> redirect:/course/introduction/liberal
    // @GetMapping("/introduction/arts")     -> redirect:/course/introduction/arts
}
