package com.lms.mainpages.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/info")
public class InfoController {

    /**
     * 정보센터 공통 모델 설정 메서드
     * @param model Spring Model 객체
     * @param fragment Thymeleaf fragment 경로
     * @param title 페이지 제목
     * @return layout 템플릿명
     */
    private String setupInfoModel(Model model, String fragment, String title) {
        model.addAttribute("bodyFragment", fragment);
        model.addAttribute("title", title);
        model.addAttribute("showSidebar", true); // 사이드바 표시
        model.addAttribute("userRole", "guest");
        model.addAttribute("activeCategory", "info");
        return "layout";
    }

    /**
     * 정보센터 메인 페이지
     * URL: /info
     */
    @GetMapping
    public String infoPage(Model model) {
        return setupInfoModel(model,
                "info/teacherIntro :: content",  // infoContent → content로 변경
                "MyLms"
        );
    }

    /**
     * 강사소개 페이지
     * URL: /info/teacher
     */
    @GetMapping("/teacher")
    public String teacherIntro(Model model) {
        return setupInfoModel(model,
                "info/teacherIntro :: content",
                "MyLms"
        );
    }

    /**
     * 도서추천 페이지
     * URL: /info/books
     */
    @GetMapping("/books")
    public String bookRecommendation(Model model) {
        return setupInfoModel(model,
                "info/bookRecommendation :: content",
                "MyLms"
        );
    }

    /**
     * 시험일정 페이지
     * URL: /info/exam
     */
    @GetMapping("/exam")
    public String examSchedule(Model model) {
        return setupInfoModel(model,
                "info/examSchedule :: content",
                "MyLms"
        );
    }

    /**
     * 자료실 페이지
     * URL: /info/resources
     */
    @GetMapping("/resources")
    public String resources(Model model) {
        return setupInfoModel(model,
                "info/resources :: content",
                "MyLms"
        );
    }
}

