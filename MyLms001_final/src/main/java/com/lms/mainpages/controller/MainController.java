package com.lms.mainpages.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final JdbcTemplate jdbc;

    /** 레이아웃에 index 프래그먼트 꽂기 */
    private String setup(Model model, String fragment, String title) {
        model.addAttribute("bodyFragment", fragment); // "index :: indexContent"
        model.addAttribute("title", title);
        model.addAttribute("userRole", "guest");
        model.addAttribute("activeCategory", "home");
        model.addAttribute("showSidebar", false);
        return "layout";
    }

    // ❗ 루트("/")는 다른 컨트롤러와 충돌하니 제외
    @GetMapping({"/","/home", "/index"})
    public String home(Model model, HttpServletRequest req) {

        // ================= KPI 채우기 (옵션 A: 누적/최근 집계) =================
        // 오늘 학습: 최근 7일 기준 → 없으면 다른 후보 → 최종적으로 enrollments 전체 개수라도 노출
        model.addAttribute("kpiTodayStudy", safeCount(
                """
                SELECT COUNT(*) FROM project.learning_logs
                 WHERE created_at >= NOW() - INTERVAL 7 DAY
                """,
                0, true,
                // fallback 후보들 (순서대로 시도)
                """
                SELECT COUNT(*) FROM project.course_progress
                 WHERE updated_at >= NOW() - INTERVAL 7 DAY
                """,
                """
                SELECT COUNT(*) FROM project.enrollments
                 WHERE enrolled_at >= NOW() - INTERVAL 30 DAY
                """,
                "SELECT COUNT(*) FROM project.enrollments"
        ));

        // 신규 강좌: 최근 30일 (updated_at 없으면 created_at 사용), 없으면 전체 강좌 수
        model.addAttribute("kpiNewCourses", safeCount(
                """
                SELECT COUNT(*)
                  FROM project.courses
                 WHERE COALESCE(updated_at, created_at, NOW())
                       >= NOW() - INTERVAL 30 DAY
                """,
                0, true,
                "SELECT COUNT(*) FROM project.courses"
        ));

        // 공지: 삭제 제외, 누적 (항상 노출 가능)
        model.addAttribute("kpiNotices", safeCount(
                """
                SELECT COUNT(*)
                  FROM project.posts
                 WHERE UPPER(category)='NOTICE'
                   AND COALESCE(is_deleted,0)=0
                """
        ));

        // 메시지: 스키마 다양성을 고려해 후보 순차 시도 (없으면 0)
        model.addAttribute("kpiMessages", safeCount(
                """
                SELECT COUNT(*) FROM project.messages
                 WHERE created_at >= NOW() - INTERVAL 7 DAY
                """,
                0, true,
                // 알림 테이블이 있다면 '읽지 않은 알림' 개수
                "SELECT COUNT(*) FROM project.notifications WHERE COALESCE(is_read,0)=0",
                "SELECT 0"
        ));
        // ================================================================

        return setup(model, "index :: indexContent", "MyLms");
    }

    // =============== 안전 카운트 유틸 (후보 쿼리 순차 시도) ===============
    private int safeCount(String primarySql) {
        return safeCount(primarySql, 0, false);
    }

    /**
     * @param primarySql   1순위 쿼리
     * @param defaultValue 실패 시 기본값
     * @param tryFallbacks true면 fallbacks를 순서대로 시도
     * @param fallbacks    대체 쿼리 목록
     */
    private int safeCount(String primarySql, int defaultValue, boolean tryFallbacks, String... fallbacks) {
        try {
            Integer v = jdbc.queryForObject(primarySql, Integer.class);
            if (v != null) return v;
        } catch (Exception ignore) {
            if (tryFallbacks) {
                for (String fb : fallbacks) {
                    try {
                        Integer v = jdbc.queryForObject(fb, Integer.class);
                        if (v != null) return v;
                    } catch (Exception ignored) { /* 다음 후보 시도 */ }
                }
            }
        }
        return defaultValue;
    }
    // ====================================================================
}
