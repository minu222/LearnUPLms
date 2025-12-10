package com.lms.mainpages.service;

import com.lms.mainpages.api.dto.ReviewDetailDto;
import com.lms.mainpages.api.dto.ReviewListItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReviewService {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public ReviewService(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public static final class ReviewSchema {
        final String table;       // reviews
        final String id;          // review_id (PK, auto)
        final String instructor;  // instructor_id (nullable)
        final String student;     // student_id
        final String course;      // course_id
        final String title;       // title
        final String content;     // content
        final String category;    // category (ENUM)
        final String score;       // score (nullable)
        final String createdAt;   // created_at
        final String updatedAt;   // updated_at
        ReviewSchema(String table, String id, String instructor, String student, String course,
                     String title, String content, String category, String score,
                     String createdAt, String updatedAt) {
            this.table = table; this.id = id; this.instructor = instructor; this.student = student; this.course = course;
            this.title = title; this.content = content; this.category = category; this.score = score;
            this.createdAt = createdAt; this.updatedAt = updatedAt;
        }
    }

    private volatile ReviewSchema cached;

    private static String pick(Set<String> cols, String... cands) {
        for (String c : cands) if (cols.contains(c)) return c;
        return null;
    }

    private ReviewSchema resolve() {
        var c = cached;
        if (c != null) return c;

        synchronized (this) {
            if (cached != null) return cached;

            String[] tableCands = {"reviews","course_reviews","review"};
            try (Connection con = dataSource.getConnection()) {
                String table = null;
                for (String t : tableCands) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT 1 FROM information_schema.tables WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) { table = t; break; }
                        }
                    }
                }
                if (table == null) throw new IllegalStateException("리뷰 테이블을 찾지 못했습니다.");

                Set<String> cols = new HashSet<>();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT column_type, column_name FROM information_schema.columns WHERE table_schema=database() AND table_name=?")) {
                    ps.setString(1, table);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) cols.add(rs.getString("column_name").toLowerCase(Locale.ROOT));
                    }
                }

                String id        = pick(cols, "review_id","id");
                String instructor= pick(cols, "instructor_id","teacher_id");
                String student   = pick(cols, "student_id","user_id","member_id");
                String course    = pick(cols, "course_id");
                String title     = pick(cols, "title","subject");
                String content   = pick(cols, "content","body","text");
                String category  = pick(cols, "category","type"); // ENUM
                String score     = pick(cols, "score","rating","star");
                String createdAt = pick(cols, "created_at","created","reg_date","write_date");
                String updatedAt = pick(cols, "updated_at","updated","mod_date");

                cached = new ReviewSchema(table, id, instructor, student, course, title, content,
                        category, score, createdAt, updatedAt);
                System.out.println("[ReviewService] resolved table=" + table + ", id=" + id + ", student=" + student + ", course=" + course + ", category=" + category);
                return cached;
            } catch (SQLException e) {
                throw new IllegalStateException("리뷰 테이블 메타 탐지 실패", e);
            }
        }
    }

    // ===== ENUM 옵션 로딩 =====
    private List<String> loadEnumOptions(String table, String column) {
        if (table == null || column == null) return List.of();
        String sql = "SELECT column_type FROM information_schema.columns WHERE table_schema=database() AND table_name=? AND column_name=?";
        String colType = jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, table, column);
        if (colType == null || !colType.toLowerCase(Locale.ROOT).startsWith("enum")) return List.of();
        List<String> opts = new ArrayList<>();
        Matcher m = Pattern.compile("'([^']*)'").matcher(colType);
        while (m.find()) opts.add(m.group(1));
        return opts;
    }
    private String courseCategoryValue(ReviewSchema s) {
        if (s.category == null) return null;
        var opts = loadEnumOptions(s.table, s.category);
        if (opts.isEmpty()) return null;
        for (String v : opts) if ("강의후기".equals(v)) return v;
        for (String v : opts) if (v.contains("강의")) return v;
        for (String v : opts) if ("COURSE".equalsIgnoreCase(v) || "COURSE_REVIEW".equalsIgnoreCase(v)) return v;
        return opts.get(0);
    }
    private String teacherCategoryValue(ReviewSchema s) {
        if (s.category == null) return null;
        var opts = loadEnumOptions(s.table, s.category);
        if (opts.isEmpty()) return null;
        for (String v : opts) if ("강사후기".equals(v)) return v;
        for (String v : opts) if (v.contains("강사")) return v;
        for (String v : opts) if ("TEACHER".equalsIgnoreCase(v) || "TEACHER_REVIEW".equalsIgnoreCase(v) || "INSTRUCTOR_REVIEW".equalsIgnoreCase(v)) return v;
        return opts.get(0);
    }

    private Timestamp nowTs() { return new Timestamp(System.currentTimeMillis()); }

    // ===== 등록: 강의 후기 =====
    public int insertCourseReview(Long studentId, Long courseId, Long instructorId,
                                  String courseTitle, String content, Integer scoreNullable) {
        ReviewSchema s = resolve();

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        if (s.student != null)    { cols.add(s.student);     vals.add(studentId); }
        if (s.course  != null)    { cols.add(s.course);      vals.add(courseId); }
        if (s.instructor != null) { cols.add(s.instructor);  vals.add(instructorId); }
        if (s.title   != null)    { cols.add(s.title);       vals.add(courseTitle); }
        if (s.content != null)    { cols.add(s.content);     vals.add(content); }

        String catVal = courseCategoryValue(s);
        if (s.category != null && catVal != null) { cols.add(s.category); vals.add(catVal); }

        if (s.score   != null)    { cols.add(s.score);       vals.add(scoreNullable); }

        // created_at / updated_at 명시 저장(컬럼이 있으면)
        Timestamp now = nowTs();
        if (s.createdAt != null)  { cols.add(s.createdAt);   vals.add(now); }
        if (s.updatedAt != null)  { cols.add(s.updatedAt);   vals.add(now); }

        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + s.table + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        return jdbc.update(sql, vals.toArray());
    }

    // ===== 등록: 강사 후기 =====
    public int insertTeacherReview(Long studentId, Long courseId, Long instructorId,
                                   String title, String content, Integer scoreNullable) {
        ReviewSchema s = resolve();

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        if (s.student != null)    { cols.add(s.student);     vals.add(studentId); }
        if (s.course  != null)    { cols.add(s.course);      vals.add(courseId); }
        if (s.instructor != null) { cols.add(s.instructor);  vals.add(instructorId); }
        if (s.title   != null)    { cols.add(s.title);       vals.add(title); }
        if (s.content != null)    { cols.add(s.content);     vals.add(content); }

        String catVal = teacherCategoryValue(s);
        if (s.category != null && catVal != null) { cols.add(s.category); vals.add(catVal); }

        if (s.score   != null)    { cols.add(s.score);       vals.add(scoreNullable); }

        // created_at / updated_at 명시 저장
        Timestamp now = nowTs();
        if (s.createdAt != null)  { cols.add(s.createdAt);   vals.add(now); }
        if (s.updatedAt != null)  { cols.add(s.updatedAt);   vals.add(now); }

        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + s.table + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        return jdbc.update(sql, vals.toArray());
    }

    // ===== 목록: 강의 후기 =====
    public List<ReviewListItem> listCourseReviews(String q, String filter) {
        ReviewSchema s = resolve();
        String catVal = courseCategoryValue(s);

        StringBuilder sql = new StringBuilder()
                .append("SELECT ")
                .append(s.id).append(" AS id, ")
                .append(s.title).append(" AS title, ")
                .append(s.createdAt).append(" AS created_at, ")
                .append(s.student).append(" AS student_id ")
                .append("FROM ").append(s.table).append(" WHERE 1=1 ");

        List<Object> args = new ArrayList<>();

        if (s.category != null && catVal != null) {
            sql.append(" AND ").append(s.category).append(" = ? ");
            args.add(catVal);
        }

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            boolean hasTitle = s.title != null;
            boolean hasContent = s.content != null;
            if (hasTitle || hasContent) {
                sql.append(" AND (1=0 ");
                if (hasTitle)   { sql.append(" OR ").append(s.title).append(" LIKE ? ");   args.add(like); }
                if (hasContent) { sql.append(" OR ").append(s.content).append(" LIKE ? "); args.add(like); }
                sql.append(") ");
            }
        }

        sql.append(" ORDER BY ").append(s.createdAt != null ? s.createdAt : s.id).append(" DESC ")
                .append(" LIMIT 200 ");

        return jdbc.query(sql.toString(), rs -> {
            List<ReviewListItem> out = new ArrayList<>();
            while (rs.next()) {
                ReviewListItem it = new ReviewListItem();
                it.setId(rs.getLong("id"));
                it.setTitle(rs.getString("title"));
                Timestamp ts = null;
                try { ts = rs.getTimestamp("created_at"); } catch (SQLException ignored) {}
                it.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
                long sid = 0L;
                try { sid = rs.getLong("student_id"); } catch (SQLException ignored) {}
                it.setStudentId(sid);
                it.setAuthorName(sid > 0 ? ("회원#" + sid) : "회원");
                out.add(it);
            }
            return out;
        }, args.toArray());
    }

    // ===== 상세: 강의 후기 =====
    public ReviewDetailDto getCourseReview(Long reviewId) {
        ReviewSchema s = resolve();
        String sql = "SELECT "
                + s.id + " AS id, "
                + s.title + " AS title, "
                + s.content + " AS content, "
                + s.createdAt + " AS created_at, "
                + s.student + " AS student_id "
                + "FROM " + s.table + " WHERE " + s.id + " = ?";

        List<ReviewDetailDto> list = jdbc.query(sql, (rs, i) -> {
            ReviewDetailDto d = new ReviewDetailDto();
            d.setId(rs.getLong("id"));
            d.setTitle(rs.getString("title"));
            d.setContent(rs.getString("content"));
            Timestamp ts = null;
            try { ts = rs.getTimestamp("created_at"); } catch (SQLException ignored) {}
            d.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
            long sid = 0L;
            try { sid = rs.getLong("student_id"); } catch (SQLException ignored) {}
            d.setStudentId(sid);
            d.setAuthorName(sid > 0 ? ("회원#" + sid) : "회원");
            return d;
        }, reviewId);

        return list.isEmpty() ? null : list.get(0);
    }
}
