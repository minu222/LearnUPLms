package com.lms.adminpages.stats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sales")
public class StatController {


    @GetMapping("/annual-revenue-stats")
    public String annualRevenue() {
        return "adminpages/annual-revenue-stats/index";
    }


    @GetMapping("/monthly-revenue-stats")
    public String  monthlyRevenue() {
        return "adminpages/monthly-revenue-stats/index";
    }

    @GetMapping("/course-revenue-stats")
    public String  courseRevenue() {
        return "adminpages/course-revenue-stats/index";
    }

    @GetMapping("/tax-management")
    public String taxManagement() {
        return "adminpages/tax-management/index";
    }
}
