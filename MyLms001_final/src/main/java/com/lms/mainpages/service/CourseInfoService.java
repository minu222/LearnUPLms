package com.lms.mainpages.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class CourseInfoService {

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public CourseInfoService(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    /* ======================= 강의 테이블 해석 ======================= */

    private static final class Resolved {
        final String table;       // courses
        final String idCol;       // course_id / id / class_id
        final String titleCol;    // title / course_name / name
        final String instructorCol; // instructor_id / teacher_id / user_id ...
        Resolved(String table, String idCol, String titleCol, String instructorCol) {
            this.table = table; this.idCol = idCol; this.titleCol = titleCol; this.instructorCol = instructorCol;
        }
    }

    private volatile Resolved cached;

    private static String pick(Set<String> cols, String... cands) {
        for (String c : cands) if (cols.contains(c)) return c;
        return null;
    }

    private Resolved resolve() {
        var c = cached;
        if (c != null) return c;

        synchronized (this) {
            if (cached != null) return cached;

            String[] tableCands = {"courses","course","classes","class","tbl_course","tb_courses"};
            String[] idCands    = {"course_id","id","class_id"};
            String[] titleCands = {"title","course_name","name"};
            String[] instrCands = {"instructor_id","teacher_id","user_id","owner_id"};

            try (Connection con = dataSource.getConnection()) {
                for (String t : tableCands) {
                    // 테이블 존재 확인
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT 1 FROM information_schema.tables WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) continue;
                        }
                    }
                    // 컬럼 수집
                    Set<String> cols = new HashSet<>();
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT column_name FROM information_schema.columns WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) cols.add(rs.getString(1).toLowerCase(Locale.ROOT));
                        }
                    }
                    String id    = pick(cols, idCands);
                    String title = pick(cols, titleCands);
                    String instr = pick(cols, instrCands); // optional
                    if (id != null && title != null) {
                        cached = new Resolved(t, id, title, instr);
                        System.out.println("[CourseInfoService] resolved table=" + t + ", id=" + id + ", title=" + title + ", instructor=" + instr);
                        return cached;
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException("강의 테이블 메타 탐지 실패", e);
            }
            throw new IllegalStateException("강의 테이블/컬럼을 찾지 못했습니다.");
        }
    }

    /* ======================= 사용자(강사) 테이블 해석 ======================= */

    private static final class UserResolved {
        final String table;    // users/members 등
        final String idCol;    // user_id / id / member_id
        final String nameCol;  // name / username / full_name / display_name
        UserResolved(String table, String idCol, String nameCol) {
            this.table = table; this.idCol = idCol; this.nameCol = nameCol;
        }
    }

    private volatile UserResolved cachedUser;

    private UserResolved resolveUser() {
        var cu = cachedUser;
        if (cu != null) return cu;

        synchronized (this) {
            if (cachedUser != null) return cachedUser;

            String[] tableCands = {"users","user","members","member","tbl_user","tb_users","tb_member","tbl_member"};
            String[] idCands    = {"user_id","id","member_id"};
            String[] nameCands  = {"name","username","full_name","display_name","user_name"};

            try (Connection con = dataSource.getConnection()) {
                for (String t : tableCands) {
                    // 테이블 존재?
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT 1 FROM information_schema.tables WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) continue;
                        }
                    }
                    // 컬럼 확인
                    Set<String> cols = new HashSet<>();
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT column_name FROM information_schema.columns WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) cols.add(rs.getString(1).toLowerCase(Locale.ROOT));
                        }
                    }
                    String id   = pick(cols, idCands);
                    String name = pick(cols, nameCands);
                    if (id != null && name != null) {
                        cachedUser = new UserResolved(t, id, name);
                        System.out.println("[CourseInfoService] resolved userTable=" + t + ", id=" + id + ", name=" + name);
                        return cachedUser;
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException("사용자 테이블 메타 탐지 실패", e);
            }
            // 사용자 테이블이 없으면 null 허용(이름 조회만 실패)
            return cachedUser = new UserResolved(null, null, null);
        }
    }

    /* ======================= 조회 API ======================= */

    public String findCourseTitle(Long courseId) {
        if (courseId == null) return null;
        var r = resolve();
        String sql = "SELECT " + r.titleCol + " FROM " + r.table + " WHERE " + r.idCol + "=? LIMIT 1";
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, courseId);
    }

    public Long findInstructorIdOfCourse(Long courseId) {
        if (courseId == null) return null;
        var r = resolve();
        if (r.instructorCol == null) return null;
        String sql = "SELECT " + r.instructorCol + " FROM " + r.table + " WHERE " + r.idCol + "=? LIMIT 1";
        return jdbc.query(sql, rs -> rs.next() ? rs.getLong(1) : null, courseId);
    }

    /** 강의ID로 강사 이름 조회: 강의→강사ID→사용자테이블 이름 */
    public String findInstructorNameOfCourse(Long courseId) {
        if (courseId == null) return null;
        Long instructorId = findInstructorIdOfCourse(courseId);
        if (instructorId == null) return null;

        var u = resolveUser();
        if (u.table == null || u.idCol == null || u.nameCol == null) return null;

        String sql = "SELECT " + u.nameCol + " FROM " + u.table + " WHERE " + u.idCol + "=? LIMIT 1";
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, instructorId);
    }

    /** 수강 만료 여부: enrollments(expired_at <= NOW()) 기준 */
    private static final class EnrollResolved {
        final String table;        // enrollments
        final String studentCol;   // student_id / user_id / member_id
        final String courseCol;    // course_id
        final String expiredAtCol; // expired_at / end_at / finished_at
        final String enrolledAtCol;// enrolled_at (정렬용, 없으면 null)
        EnrollResolved(String table, String studentCol, String courseCol, String expiredAtCol, String enrolledAtCol) {
            this.table = table; this.studentCol = studentCol; this.courseCol = courseCol;
            this.expiredAtCol = expiredAtCol; this.enrolledAtCol = enrolledAtCol;
        }
    }
    private volatile EnrollResolved cachedEnroll;

    private EnrollResolved resolveEnroll() {
        var c = cachedEnroll;
        if (c != null) return c;
        synchronized (this) {
            if (cachedEnroll != null) return cachedEnroll;

            String[] tableCands = {"enrollments","enrollment","registrations","course_enrollments","tbl_enrollments","tb_enrollments"};
            String[] studentCands = {"student_id","user_id","member_id"};
            String[] courseCands  = {"course_id","class_id"};
            String[] expiredCands = {"expired_at","end_at","finished_at","expires_at"};
            String[] enrolledCands= {"enrolled_at","created_at","reg_date"};

            try (Connection con = dataSource.getConnection()) {
                for (String t : tableCands) {
                    // 존재 확인
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT 1 FROM information_schema.tables WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) continue; }
                    }
                    // 컬럼 수집
                    Set<String> cols = new HashSet<>();
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT column_name FROM information_schema.columns WHERE table_schema=database() AND table_name=?")) {
                        ps.setString(1, t);
                        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) cols.add(rs.getString(1).toLowerCase(Locale.ROOT)); }
                    }
                    String sCol = pick(cols, studentCands);
                    String cCol = pick(cols, courseCands);
                    String xCol = pick(cols, expiredCands);
                    String eCol = pick(cols, enrolledCands); // 없으면 null 허용
                    if (sCol != null && cCol != null && xCol != null) {
                        cachedEnroll = new EnrollResolved(t, sCol, cCol, xCol, eCol);
                        System.out.println("[CourseInfoService] resolved enroll table=" + t + " student=" + sCol + " course=" + cCol + " expired=" + xCol);
                        break;
                    }
                }
            } catch (SQLException e) {
                // fallthrough -> cachedEnroll remains null
            }
            if (cachedEnroll == null) cachedEnroll = new EnrollResolved(null,null,null,null,null);
            return cachedEnroll;
        }
    }

    /** 수강 만료 여부 (expired_at <= NOW()) */
    public Boolean isEnrollmentExpired(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) return null;
        var er = resolveEnroll();
        if (er.table == null) return null; // 탐지 실패 -> null

        StringBuilder sql = new StringBuilder()
                .append("SELECT (CASE WHEN ").append(er.expiredAtCol).append(" IS NOT NULL AND ")
                .append(er.expiredAtCol).append(" <= NOW() THEN 1 ELSE 0 END) AS expired ")
                .append("FROM ").append(er.table).append(" ")
                .append("WHERE ").append(er.studentCol).append(" = ? AND ").append(er.courseCol).append(" = ? ");

        if (er.enrolledAtCol != null) sql.append("ORDER BY ").append(er.enrolledAtCol).append(" DESC ");
        sql.append("LIMIT 1");

        Integer v = jdbc.query(sql.toString(), rs -> rs.next() ? rs.getInt("expired") : null, studentId, courseId);
        return v != null && v == 1;
    }


}
