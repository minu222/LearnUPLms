package com.lms.adminpages;

import com.lms.adminpages.dashboard.entity.Dashboard;
import com.lms.adminpages.dashboard.service.DashboardService;
import com.lms.adminpages.dashboard.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {



    private final DashboardService dashboardService;
    private final NotificationService notificationService;


    public HomeController(DashboardService dashboardService, NotificationService notificationService) {
        this.dashboardService = dashboardService;
        this.notificationService = notificationService;
    }

    @GetMapping("/admin/home")
    public String home(Model model) {
        Dashboard data = dashboardService.getDashboardData();

        model.addAttribute("studentCount", data.getStudentCount());
        model.addAttribute("teacherCount", data.getTeacherCount());
        model.addAttribute("courseCount", data.getCourseCount());
        model.addAttribute("avgCompletion", data.getAvgCompletion());
        model.addAttribute("cancelRate", data.getCancelRate());
        model.addAttribute("monthRevenue", data.getMonthRevenue());

        // 최근 알림 3개
        model.addAttribute("notices", notificationService.getRecentNotifications());

        return "adminpages/home";
    }
}