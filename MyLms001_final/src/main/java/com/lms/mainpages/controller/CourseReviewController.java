// src/main/java/dwacademy/mylms001/controller/CourseReviewController.java
package com.lms.mainpages.controller;

import com.lms.mainpages.service.CourseInfoService;
import com.lms.mainpages.service.ReviewService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.Locale;

@RestController
@RequestMapping("/review")
public class CourseReviewController {

    private final CourseInfoService courseInfoService;
    private final ReviewService reviewService;

    public CourseReviewController(CourseInfoService courseInfoService, ReviewService reviewService) {
        this.courseInfoService = courseInfoService;
        this.reviewService = reviewService;
    }

    /* ================= 공통 헬퍼 ================= */

    // alert 후 history.back()을 수행하는 전체 HTML 응답
    private ResponseEntity<String> alertBack(String msg) {
        String html = "<!doctype html><html lang='ko'><head><meta charset='UTF-8'></head>"
                + "<body><script>alert('" + JavaScriptUtils.javaScriptEscape(msg) + "');history.back();</script></body></html>";
        return ResponseEntity.status(200)
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(html);
    }

    // 다양한 세션 보관 키를 지원하여 로그인 사용자 ID 추출
    private Long toLongOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            String s = String.valueOf(o).trim();
            return s.isEmpty() ? null : Long.parseLong(s);
        } catch (Exception ignored) { return null; }
    }
    private Object resolvePrincipal(HttpSession session) {
        if (session == null) return null;
        for (String k : new String[]{"loginUser", "user", "member", "principal", "currentUser", "authUser"}) {
            Object v = session.getAttribute(k);
            if (v != null) return v;
        }
        return null;
    }
    private Long extractId(Object obj) {
        if (obj == null) return null;
        String[] getters = {"getUserId", "getUser_id", "getId", "getMemberId", "getMember_id", "getUserNo", "getSeq", "getNo"};
        for (String g : getters) {
            try {
                Method m = obj.getClass().getMethod(g);
                Long v = toLongOrNull(m.invoke(obj));
                if (v != null) return v;
            } catch (Exception ignored) {}
        }
        // nested
        for (String nest : new String[]{"getUser", "getMember", "getAccount", "getPrincipal"}) {
            try {
                Method m = obj.getClass().getMethod(nest);
                Object n = m.invoke(obj);
                if (n != null) {
                    Long v = extractId(n);
                    if (v != null) return v;
                }
            } catch (Exception ignored) {}
        }
        // heuristic
        try {
            for (Method m : obj.getClass().getMethods()) {
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (!n.startsWith("get") || m.getParameterCount() != 0) continue;
                if (n.endsWith("id") || n.endsWith("no") || n.endsWith("seq")) {
                    Long v = toLongOrNull(m.invoke(obj));
                    if (v != null) return v;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    private Long resolveStudentId(HttpSession session) { return extractId(resolvePrincipal(session)); }

    /* ================= 강의 후기 ================= */

    // GET: 강의 후기 등록 폼 (만료 전 진입 차단)
    @GetMapping(value = "/course/{courseId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> courseReviewForm(@PathVariable("courseId") Long courseId,
                                                   HttpServletRequest req) {
        Long studentId = resolveStudentId(req.getSession(false));
        Boolean expired = courseInfoService.isEnrollmentExpired(studentId, courseId);
        if (expired != null && !expired) {
            return alertBack("수강 기간이 만료된 이후에만 후기를 작성할 수 있습니다.");
        }

        String title = courseInfoService.findCourseTitle(courseId);
        if (title == null || title.isBlank()) title = "해당 강의명";
        String safeTitle = HtmlUtils.htmlEscape(title);

        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"ko\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>강의 후기 등록 | MyLMS</title>" +
                "<style>body{margin:0;font-family:'Noto Sans KR',sans-serif;background-color:#f5f6fa;color:#333}" +
                ".container{max-width:700px;margin:60px auto;background:#fff;border-radius:12px;padding:40px;box-shadow:0 8px 24px rgba(0,0,0,.12)}" +
                "h2{margin-top:0;font-size:1.8em;font-weight:700;color:#1f2937;margin-bottom:30px}" +
                ".form-group{margin-bottom:25px}label{display:block;margin-bottom:10px;font-weight:600;color:#374151}" +
                "input[readonly],textarea{width:100%;padding:14px;border:1px solid #d1d5db;border-radius:8px;font-size:1em;color:#111827;background-color:#f9fafb}" +
                "input[readonly]{background-color:#e5e7eb}textarea{min-height:220px;resize:vertical}" +
                "button{display:block;margin-left:auto;padding:14px 28px;background-color:#f59e0b;border:none;border-radius:8px;font-weight:700;color:#fff;cursor:pointer;transition:.3s}" +
                "button:hover{background-color:#d97706}</style></head><body>" +
                "<div class=\"container\"><h2>강의 후기 등록</h2>" +
                "<form method=\"post\" action=\"/review/course/" + HtmlUtils.htmlEscape(String.valueOf(courseId)) + "\">" +
                "<div class=\"form-group\"><input type=\"text\" name=\"kind\" value=\"강의 후기\" readonly></div>" +
                "<div class=\"form-group\"><label>제목</label><input type=\"text\" name=\"title\" value=\"" + safeTitle + "\" readonly></div>" +
                "<div class=\"form-group\"><label>내용</label><textarea name=\"content\" placeholder=\"내용을 작성하세요\"></textarea></div>" +
                "<button type=\"submit\">등록</button></form></div></body></html>";

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8").body(html);
    }

    // POST: 강의 후기 등록 처리 (만료 검증 포함)
    @PostMapping(value = "/course/{courseId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> submitCourseReview(@PathVariable("courseId") Long courseId,
                                                     @RequestParam(value = "title",   required = false) String title,
                                                     @RequestParam(value = "content", required = false) String content,
                                                     HttpSession session) {

        Long studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(401).body("<!doctype html><html><body><script>alert('로그인이 필요합니다.');location.href='/user/login';</script></body></html>");
        }

        // ✅ 만료 검증
        Boolean expired = courseInfoService.isEnrollmentExpired(studentId, courseId);
        if (expired == null || !expired) {
            return alertBack("수강 기간이 만료된 이후에만 후기를 작성할 수 있습니다.");
        }

        if (title == null || title.isBlank()) {
            title = courseInfoService.findCourseTitle(courseId);
            if (title == null || title.isBlank()) title = "강의 후기";
        }
        if (content == null) content = "";

        Long instructorId = courseInfoService.findInstructorIdOfCourse(courseId);
        Integer score = null; // 점수 UI 없음 → NULL

        int rows = reviewService.insertCourseReview(studentId, courseId, instructorId, title, content, score);

        String html = (rows > 0)
                ? "<!doctype html><html><body><script>alert('후기가 등록되었습니다.');window.location.href='/myclass/studentCourses';</script></body></html>"
                : "<!doctype html><html><body><script>alert('등록에 실패했습니다.');history.back();</script></body></html>";

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8").body(html);
    }

    /* ================= 강사 후기 ================= */

    // GET: 강사 후기 등록 폼 (만료 전 진입 차단)
    @GetMapping(value = "/instructor/{courseId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> instructorReviewForm(@PathVariable("courseId") Long courseId,
                                                       HttpServletRequest req) {
        Long studentId = resolveStudentId(req.getSession(false));
        Boolean expired = courseInfoService.isEnrollmentExpired(studentId, courseId);
        if (expired != null && !expired) {
            return alertBack("수강 기간이 만료된 이후에만 후기를 작성할 수 있습니다.");
        }

        String teacher = courseInfoService.findInstructorNameOfCourse(courseId);
        if (teacher == null || teacher.isBlank()) teacher = "해당 강사";
        String safeTeacher = HtmlUtils.htmlEscape(teacher);

        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"ko\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>강사 후기 등록 | MyLMS</title>" +
                "<style>body{margin:0;font-family:'Noto Sans KR',sans-serif;background-color:#f5f6fa;color:#333}" +
                ".container{max-width:700px;margin:60px auto;background:#fff;border-radius:12px;padding:40px;box-shadow:0 8px 24px rgba(0,0,0,.12)}" +
                "h2{margin-top:0;font-size:1.8em;font-weight:700;color:#1f2937;margin-bottom:30px}" +
                ".form-group{margin-bottom:25px}label{display:block;margin-bottom:10px;font-weight:600;color:#374151}" +
                "input[readonly],textarea{width:100%;padding:14px;border:1px solid #d1d5db;border-radius:8px;font-size:1em;color:#111827;background-color:#f9fafb}" +
                "input[readonly]{background-color:#e5e7eb}textarea{min-height:220px;resize:vertical}" +
                "button{display:block;margin-left:auto;padding:14px 28px;background-color:#10b981;border:none;border-radius:8px;font-weight:700;color:#fff;cursor:pointer;transition:.3s}" +
                "button:hover{background-color:#059669}</style></head><body>" +
                "<div class=\"container\"><h2>강사 후기 등록</h2>" +
                "<form method=\"post\" action=\"/review/instructor/" + HtmlUtils.htmlEscape(String.valueOf(courseId)) + "\">" +
                "<div class=\"form-group\"><input type=\"text\" name=\"kind\" value=\"강사 후기\" readonly></div>" +
                "<div class=\"form-group\"><label>제목</label><input type=\"text\" name=\"title\" value=\"" + safeTeacher + " 강사 후기\" readonly></div>" +
                "<div class=\"form-group\"><label>내용</label><textarea name=\"content\" placeholder=\"내용을 작성하세요\"></textarea></div>" +
                "<button type=\"submit\">등록</button></form></div></body></html>";

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8").body(html);
    }

    // POST: 강사 후기 등록 처리 (만료 검증 포함)
    @PostMapping(value = "/instructor/{courseId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> submitInstructorReview(@PathVariable("courseId") Long courseId,
                                                         @RequestParam(value = "title",   required = false) String title,
                                                         @RequestParam(value = "content", required = false) String content,
                                                         HttpSession session) {

        Long studentId = resolveStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(401).body("<!doctype html><html><body><script>alert('로그인이 필요합니다.');location.href='/user/login';</script></body></html>");
        }

        // ✅ 만료 검증
        Boolean expired = courseInfoService.isEnrollmentExpired(studentId, courseId);
        if (expired == null || !expired) {
            return alertBack("수강 기간이 만료된 이후에만 후기를 작성할 수 있습니다.");
        }

        Long instructorId = courseInfoService.findInstructorIdOfCourse(courseId);
        if (title == null || title.isBlank()) {
            String teacher = courseInfoService.findInstructorNameOfCourse(courseId);
            title = (teacher == null || teacher.isBlank()) ? "강사 후기" : (teacher + " 강사 후기");
        }
        if (content == null) content = "";

        Integer score = null; // 점수 입력 없음 → NULL 저장

        int rows = reviewService.insertTeacherReview(studentId, courseId, instructorId, title, content, score);

        String html = (rows > 0)
                ? "<!doctype html><html><body><script>alert('강사 후기가 등록되었습니다.');window.location.href='/myclass/studentCourses';</script></body></html>"
                : "<!doctype html><html><body><script>alert('등록에 실패했습니다.');history.back();</script></body></html>";

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8").body(html);
    }
}
