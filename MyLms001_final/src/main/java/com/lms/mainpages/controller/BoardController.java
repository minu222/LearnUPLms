package com.lms.mainpages.controller;

import com.lms.mainpages.board.BoardType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final JdbcTemplate jdbcTemplate;

    /* ---------------- 공통 유틸 ---------------- */

    /** layout.html 은 ~{${bodyFragment}} 로 삽입하므로
     *  bodyFragment 에는 "board/경로 :: content" 를 그대로 넣는다.
     */
    private String setupBoardModel(Model model, String fragment, String title) {
        model.addAttribute("bodyFragment", fragment); // 예: "board/qnaBoard :: content"
        model.addAttribute("title", title);
        model.addAttribute("showSidebar", true);
        model.addAttribute("userRole", "guest");
        model.addAttribute("activeCategory", "board");
        return "layout";
    }

    private void setTypeSlug(Model model, BoardType bt) { model.addAttribute("typeSlug", bt.slug()); }

    private BoardType safeType(String type) {
        try { return BoardType.from(type); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown board type: " + type); }
    }

    private String toBoardTitle(BoardType bt) {
        return switch (bt) {
            case NOTICE -> "MyLms";
            case FAQ -> "MyLms";
            case FREE -> "MyLms";
            case QNA -> "MyLms";
            case TEACHER_REVIEW -> "MyLms";
            case COURSE_REVIEW -> "MyLms";
        };
    }

    /* ---------------- 공통 조회 로직 (posts) ---------------- */

    /**
     * posts 테이블에서 카테고리별 목록 조회
     * - categoryUpper: NOTICE / FAQ / FREE / QNA / COURSE_REVIEW / TEACHER_REVIEW
     * - q: 제목/내용 LIKE 검색(옵션)
     * 반환: id, title, authorName, createdAt(LocalDateTime)
     */
    private List<Map<String, Object>> queryPostsByCategory(String categoryUpper, String q) {
        final boolean hasQ = (q != null && !q.isBlank());

        final String baseSql = """
            SELECT  p.post_id      AS id,
                    p.title        AS title,
                    COALESCE(u.name, '익명') AS authorName,
                    p.created_at   AS createdAt
              FROM  project.posts p
        LEFT JOIN  project.users u ON u.user_id = p.user_id
             WHERE  UPPER(p.category) = ?
               AND  p.is_deleted = 0
        """;
        final String whereQ = hasQ ? " AND (p.title LIKE ? OR p.content LIKE ?) " : " ";
        final String order  = " ORDER BY p.post_id DESC LIMIT 200";
        final String sql    = baseSql + whereQ + order;

        Object[] args = hasQ
                ? new Object[]{ categoryUpper, "%" + q + "%", "%" + q + "%" }
                : new Object[]{ categoryUpper };

        return jdbcTemplate.query(sql, args, (rs, rowNum) -> {
            var m = new HashMap<String, Object>();
            m.put("id", rs.getLong("id"));
            m.put("title", rs.getString("title"));
            m.put("authorName", rs.getString("authorName"));
            Timestamp ts = rs.getTimestamp("createdAt");
            m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
            m.putIfAbsent("role", "USER");
            return m;
        });
    }

    /* ---------------- 전용 조회 로직 (reviews: 강의후기) ---------------- */

    /**
     * reviews 테이블에서 category='강의후기' 데이터만 조회
     * - q: 제목/내용/강의명/학생/강사 LIKE 검색(옵션)
     * 반환: id, title, excerpt, score, createdAt, courseTitle, studentName, instructorName, authorName
     */
    private List<Map<String, Object>> queryCourseReviewsOnly(String q) {
        final boolean hasQ = (q != null && !q.isBlank());

        final String baseSql = """
            SELECT  r.review_id          AS id,
                    r.title              AS title,
                    LEFT(r.content,256)  AS excerpt,
                    r.score              AS score,
                    r.created_at         AS createdAt,
                    c.title              AS courseTitle,
                    stu.name             AS studentName,
                    ins.name             AS instructorName
              FROM  project.reviews r
         LEFT JOIN  project.courses c    ON c.course_id = r.course_id
         LEFT JOIN  project.users   stu  ON stu.user_id = r.student_id
         LEFT JOIN  project.users   ins  ON ins.user_id = r.instructor_id
             WHERE  r.category = ?
        """;

        final String whereQ = hasQ
                ? " AND (r.title LIKE ? OR r.content LIKE ? OR c.title LIKE ? OR stu.name LIKE ? OR ins.name LIKE ?) "
                : " ";
        final String order  = " ORDER BY r.review_id DESC LIMIT 1000";
        final String sql    = baseSql + whereQ + order;

        Object[] args = hasQ
                ? new Object[]{ "강의후기", "%" + q + "%", "%" + q + "%", "%" + q + "%", "%" + q + "%", "%" + q + "%" }
                : new Object[]{ "강의후기" };

        return jdbcTemplate.query(sql, args, (rs, rowNum) -> {
            var m = new HashMap<String,Object>();
            m.put("id", rs.getLong("id"));
            m.put("title", rs.getString("title"));
            m.put("excerpt", rs.getString("excerpt"));
            m.put("score", rs.getObject("score")); // INT/DECIMAL 스키마 모두 안전
            Timestamp ts = rs.getTimestamp("createdAt");
            m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
            m.put("courseTitle", rs.getString("courseTitle"));
            m.put("studentName", rs.getString("studentName"));
            m.put("instructorName", rs.getString("instructorName"));

            // 템플릿 호환용 공통 필드
            String author = rs.getString("studentName");
            m.put("authorName", (author != null && !author.isBlank()) ? author : "익명");
            m.putIfAbsent("role", "USER");
            return m;
        });
    }
    private List<Map<String, Object>> queryTeacherReviewsOnly(String q) {
        final boolean hasQ = (q != null && !q.isBlank());
        final String baseSql = """
        SELECT  r.review_id          AS id,
                r.title              AS title,
                LEFT(r.content,256)  AS excerpt,
                r.score              AS score,
                r.created_at         AS createdAt,
                c.title              AS courseTitle,
                stu.name             AS studentName,
                ins.name             AS instructorName
          FROM  project.reviews r
     LEFT JOIN  project.courses c    ON c.course_id = r.course_id
     LEFT JOIN  project.users   stu  ON stu.user_id = r.student_id
     LEFT JOIN  project.users   ins  ON ins.user_id = r.instructor_id
         WHERE  r.category = ?
    """;
        final String whereQ = hasQ
                ? " AND (r.title LIKE ? OR r.content LIKE ? OR c.title LIKE ? OR stu.name LIKE ? OR ins.name LIKE ?) "
                : " ";
        final String order  = " ORDER BY r.review_id DESC LIMIT 1000";
        final String sql    = baseSql + whereQ + order;

        Object[] args = hasQ
                ? new Object[]{ "강사후기", "%"+q+"%", "%"+q+"%", "%"+q+"%", "%"+q+"%", "%"+q+"%" }
                : new Object[]{ "강사후기" };

        return jdbcTemplate.query(sql, args, (rs, rowNum) -> {
            var m = new HashMap<String,Object>();
            m.put("id", rs.getLong("id"));
            m.put("title", rs.getString("title"));
            m.put("excerpt", rs.getString("excerpt"));
            m.put("score", rs.getObject("score"));
            var ts = rs.getTimestamp("createdAt");
            m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
            m.put("courseTitle", rs.getString("courseTitle"));
            m.put("studentName", rs.getString("studentName"));
            m.put("instructorName", rs.getString("instructorName"));
            // 목록 공통 필드
            String author = rs.getString("studentName"); // 작성자는 수강생
            m.put("authorName", (author != null && !author.isBlank()) ? author : "익명");
            m.putIfAbsent("role", "USER");
            return m;
        });
    }

    /* ---------------- 목록 ---------------- */

    @GetMapping
    public String boardRoot() { return "redirect:/board/notice"; }

    @GetMapping("/notice")
    public String noticeBoard(Model model, @RequestParam(required = false) String q) {
        setTypeSlug(model, BoardType.NOTICE);
        var posts = queryPostsByCategory("NOTICE", q);
        model.addAttribute("posts", posts);
        return setupBoardModel(model, "board/noticeBoard :: content", toBoardTitle(BoardType.NOTICE));
    }

    // 공지사항 상세
    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable Long id, Model model) {
        setTypeSlug(model, BoardType.NOTICE);

        final String sql = """
        SELECT  p.post_id    AS id,
                p.title      AS title,
                p.content    AS content,
                p.created_at AS createdAt,
                p.updated_at AS updatedAt,
                COALESCE(u.name, '관리자') AS authorName
          FROM  project.posts p
     LEFT JOIN  project.users u ON u.user_id = p.user_id
         WHERE  p.post_id = ?
           AND  UPPER(p.category) = 'NOTICE'
           AND  p.is_deleted = 0
         LIMIT  1
    """;

        Map<String, Object> post;
        try {
            post = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                var m = new HashMap<String, Object>();
                m.put("id", rs.getLong("id"));
                m.put("title", rs.getString("title"));
                m.put("content", rs.getString("content")); // board/noticeDetail.html 에서 th:utext 가능
                var cts = rs.getTimestamp("createdAt");
                var uts = rs.getTimestamp("updatedAt");
                m.put("createdAt", cts != null ? cts.toLocalDateTime() : null);
                m.put("updatedAt", uts != null ? uts.toLocalDateTime() : null);
                m.put("authorName", rs.getString("authorName"));
                m.putIfAbsent("role", "ADMIN"); // 보여줄 용도(선택)
                return m;
            }, id);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 공지사항을 찾을 수 없습니다.");
        }

        model.addAttribute("post", post);
        // 레이아웃에 board/noticeDetail 프래그먼트 주입
        return setupBoardModel(
                model,
                "board/noticeDetail :: content",
                (String) post.getOrDefault("title", "공지사항")
        );
    }

    @GetMapping("/faq")
    public String faqBoard(Model model, @RequestParam(required = false) String q) {
        setTypeSlug(model, BoardType.FAQ);
        var posts = queryPostsByCategory("FAQ", q);
        model.addAttribute("posts", posts);
        return setupBoardModel(model, "board/faqBoard :: content", toBoardTitle(BoardType.FAQ));
    }

    @GetMapping("/courseReview")
    public String courseReviewBoard(Model model, @RequestParam(required = false) String q) {
        setTypeSlug(model, BoardType.COURSE_REVIEW);
        var posts = queryCourseReviewsOnly(q); // reviews 테이블에서 강의후기만
        model.addAttribute("posts", posts);    // 템플릿 호환 위해 키는 posts 유지
        return setupBoardModel(model, "board/courseReview :: content", toBoardTitle(BoardType.COURSE_REVIEW));
    }
    // 강의후기 상세
    @GetMapping("/courseReview/{id}")
    public String courseReviewDetail(@PathVariable Long id, Model model) {
        setTypeSlug(model, BoardType.COURSE_REVIEW);

        final String sql = """
        SELECT  r.review_id          AS id,
                r.title              AS title,
                r.content            AS content,
                r.score              AS score,
                r.created_at         AS createdAt,
                c.title              AS courseTitle,
                stu.name             AS studentName,
                ins.name             AS instructorName
          FROM  project.reviews r
     LEFT JOIN  project.courses c    ON c.course_id = r.course_id
     LEFT JOIN  project.users   stu  ON stu.user_id = r.student_id
     LEFT JOIN  project.users   ins  ON ins.user_id = r.instructor_id
         WHERE  r.review_id = ?
           AND  r.category  = ?
        LIMIT 1
    """;

        Map<String, Object> post;
        try {
            post = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                var m = new HashMap<String,Object>();
                m.put("id", rs.getLong("id"));
                m.put("title", rs.getString("title"));
                m.put("content", rs.getString("content")); // courseReviewDetail.html에서 th:utext로 사용
                m.put("score", rs.getObject("score"));
                var ts = rs.getTimestamp("createdAt");
                m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
                m.put("courseTitle", rs.getString("courseTitle"));
                m.put("studentName", rs.getString("studentName"));
                m.put("instructorName", rs.getString("instructorName"));

                // 템플릿 공통 필드
                String author = rs.getString("studentName");
                m.put("authorName", (author != null && !author.isBlank()) ? author : "익명");
                m.putIfAbsent("role", "USER");
                return m;
            }, id, "강의후기");
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 강의후기를 찾을 수 없습니다.");
        }

        model.addAttribute("post", post);
        // 레이아웃에 courseReviewDetail 프래그먼트 주입
        return setupBoardModel(model, "board/courseReviewDetail :: content",
                (String) post.getOrDefault("title", "강의후기 상세"));
    }

    @GetMapping("/teacherReview")
    public String teacherReviewBoard(Model model, @RequestParam(required = false) String q) {
        setTypeSlug(model, BoardType.TEACHER_REVIEW);
        var posts = queryTeacherReviewsOnly(q); // reviews 테이블에서 강의후기만
        model.addAttribute("posts", posts);    // 템플릿 호환 위해 키는 posts 유지
        return setupBoardModel(model, "board/teacherReview :: content", toBoardTitle(BoardType.TEACHER_REVIEW));
    }

    // 강사후기 상세
    @GetMapping("/teacherReview/{id}")
    public String teacherReviewDetail(@PathVariable Long id, Model model) {
        setTypeSlug(model, BoardType.TEACHER_REVIEW);

        final String sql = """
        SELECT  r.review_id          AS id,
                r.title              AS title,
                r.content            AS content,
                r.score              AS score,
                r.created_at         AS createdAt,
                c.title              AS courseTitle,
                stu.name             AS studentName,
                ins.name             AS instructorName
          FROM  project.reviews r
     LEFT JOIN  project.courses c    ON c.course_id = r.course_id
     LEFT JOIN  project.users   stu  ON stu.user_id = r.student_id
     LEFT JOIN  project.users   ins  ON ins.user_id = r.instructor_id
         WHERE  r.review_id = ?
           AND  r.category  = ?
        LIMIT 1
    """;

        Map<String,Object> post;
        try {
            post = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                var m = new HashMap<String,Object>();
                m.put("id", rs.getLong("id"));
                m.put("title", rs.getString("title"));
                m.put("content", rs.getString("content"));
                m.put("score", rs.getObject("score"));
                var ts = rs.getTimestamp("createdAt");
                m.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
                m.put("courseTitle", rs.getString("courseTitle"));
                m.put("studentName", rs.getString("studentName"));
                m.put("instructorName", rs.getString("instructorName"));
                String author = rs.getString("studentName");
                m.put("authorName", (author != null && !author.isBlank()) ? author : "익명");
                m.putIfAbsent("role", "USER");
                return m;
            }, id, "강사후기");
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 강사후기를 찾을 수 없습니다.");
        }

        model.addAttribute("post", post);
        return setupBoardModel(model, "board/teacherReviewDetail :: content",
                (String) post.getOrDefault("title", "강사후기 상세"));
    }


    /** 자유게시판 목록 */
    @GetMapping("/free")
    public String freeBoard(Model model, @RequestParam(required = false) String q) {
        setTypeSlug(model, BoardType.FREE);
        var posts = queryPostsByCategory("FREE", q);
        model.addAttribute("posts", posts);
        return setupBoardModel(model, "board/freeBoard :: content", toBoardTitle(BoardType.FREE));
    }

    /** 문의게시판 목록 */
    @GetMapping("/qna")
    public String qnaBoard(Model model,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String f) {
        setTypeSlug(model, BoardType.QNA);
        var posts = queryPostsByCategory("QNA", q);

        // QNA 부가 필드
        for (Map<String, Object> row : posts) {
            String name = (String) row.getOrDefault("authorName", "익명");
            row.put("authorMasked", maskName(name));
            row.putIfAbsent("draft", Boolean.FALSE);
            row.putIfAbsent("secret", Boolean.FALSE);
            row.putIfAbsent("answerStatus", "-");
        }

        model.addAttribute("posts", posts);
        return setupBoardModel(model, "board/qnaBoard :: content", toBoardTitle(BoardType.QNA));
    }

    /* ---------------- 글쓰기 ---------------- */

    @GetMapping("/{type}/write")
    public String writeForm(@PathVariable String type, Model model) {
        final BoardType bt = safeType(type);
        setTypeSlug(model, bt);
        return setupBoardModel(model, "board/write :: content", toBoardTitle(bt) + " 글쓰기");
    }

    @PostMapping("/{type}/write")
    public String writeSubmit(@PathVariable String type,
                              @RequestParam String title,
                              @RequestParam String content,
                              HttpServletRequest req) {
        final BoardType bt = safeType(type);
        final String category = switch (bt) {
            case QNA -> "QNA";
            case FREE -> "FREE";
            case NOTICE -> "NOTICE";
            case FAQ -> "FAQ";
            case COURSE_REVIEW -> "COURSE_REVIEW";
            case TEACHER_REVIEW -> "TEACHER_REVIEW";
        };

        final Long userId = getLoginUserId(req);
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        final String sql = """
            INSERT INTO project.posts
                (user_id, title, content, category, is_deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, 0, NOW(), NOW())
        """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, category);
            return ps;
        }, kh);

        Long newId = (kh.getKey() != null) ? kh.getKey().longValue() : null;
        return (newId != null) ? "redirect:/board/" + bt.slug() + "/" + newId
                : "redirect:/board/" + bt.slug();
    }

    /* ---------------- 상세(제너릭 제외 규칙) ---------------- */

    // QNA, FREE는 전용 컨트롤러가 처리하도록 '제외' 정규식만 둡니다.
    @GetMapping("/{type:^(?!qna$|free$).+}/{id:\\d+}")
    public String genericDetail(@PathVariable String type, @PathVariable Long id) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상세 페이지가 없습니다.");
    }

    /* ---------------- helpers ---------------- */

    private Long getLoginUserId(HttpServletRequest req) {
        var s = req.getSession(false);
        if (s == null) return null;

        Object o = s.getAttribute("loginUser");
        if (o instanceof com.lms.mainpages.entity.User u) {
            try { return Long.valueOf(u.getUser_id()); } catch (Exception ignored) {}
        }
        Object v = s.getAttribute("userId");
        if (v instanceof Number n) return n.longValue();
        try { return (v == null) ? null : Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) return "익명";
        String n = name.trim();
        if (n.length() <= 1) return n;
        if (n.length() == 2) return n.charAt(0) + "*";
        return n.charAt(0) + "*".repeat(n.length() - 2) + n.charAt(n.length() - 1);
    }
}
