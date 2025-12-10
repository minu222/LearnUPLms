package com.lms.adminpages.classrooms.dao;

import com.lms.adminpages.classrooms.entity.Classroom;
import com.lms.adminpages.classrooms.entity.CourseFilter;
import com.lms.adminpages.classrooms.entity.StudentDto;
import com.lms.adminpages.users.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ClassroomDAO {

    private final JdbcTemplate jdbcTemplate;
    public ClassroomDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Classroom classroom) {
        String sql = "INSERT INTO courses (instructor_id, title, description, category, price, is_free, avg_rating, status, student_count, expiry_date, live_limit) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                classroom.getInstructorId(),
                classroom.getTitle(),
                classroom.getDescription(),
                classroom.getCategory(),
                classroom.getPrice(),
                classroom.getIsFree(),
                classroom.getAvgRating(),
                classroom.getStatus(),
                classroom.getStudentCount(),
                classroom.getExpiryDate(),
                classroom.getLiveLimit()
        );
    }

    public List<String> findAllCategories() {
        String sql = "SELECT DISTINCT category FROM courses";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<Classroom> findByFilterFromDB(CourseFilter filter) {
        StringBuilder sql = new StringBuilder(
                "SELECT course_id, instructor_id, title, category, status, student_count FROM courses WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
            sql.append(" AND category = ?");
            params.add(filter.getCategory());
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append(" AND status = ?");
            params.add(filter.getStatus());
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + filter.getKeyword() + "%");
        }

        sql.append(" ORDER BY course_id DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> Classroom.builder()
                .classroomId(rs.getInt("course_id"))
                .title(rs.getString("title"))
                .category(rs.getString("category"))
                .status(rs.getString("status"))
                .studentCount(rs.getInt("student_count"))
                .instructorId(rs.getInt("instructor_id"))
                .build()
        );
    }

    public List<User> findAllInstructors() {
        String sql = "SELECT user_id, nickname FROM users WHERE role = 'instructor'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> User.builder()
                .user_id(rs.getInt("user_id"))
                .nickname(rs.getString("nickname"))
                .build()
        );
    }



    public List<Classroom> findAll() {
        String sql = """
        SELECT c.course_id, c.title, c.category, c.status, c.instructor_id,
               u.nickname AS instructorNickname,
               COUNT(e.student_id) AS studentCount
        FROM courses c
        LEFT JOIN users u ON c.instructor_id = u.user_id AND u.role = 'instructor'
        LEFT JOIN enrollments e ON c.course_id = e.course_id
        WHERE c.deleted_at IS NULL
        GROUP BY c.course_id, u.nickname
        ORDER BY c.course_id DESC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> Classroom.builder()
                .classroomId(rs.getInt("course_id"))
                .title(rs.getString("title"))
                .category(rs.getString("category"))
                .status(rs.getString("status"))
                .instructorId(rs.getInt("instructor_id"))
                .instructorNickname(rs.getString("instructorNickname"))
                .studentCount(rs.getInt("studentCount"))
                .build()
        );
    }


    public Classroom findByName(String title) {
        String sql = "SELECT course_id, title, category, status, student_count, instructor_id " +
                "FROM courses WHERE title LIKE ? AND deleted_at IS NULL LIMIT 1";
        List<Classroom> result = jdbcTemplate.query(sql, new Object[]{"%" + title + "%"}, (rs, rowNum) ->
                Classroom.builder()
                        .classroomId(rs.getInt("course_id"))
                        .title(rs.getString("title"))
                        .category(rs.getString("category"))
                        .status(rs.getString("status"))
                        .studentCount(rs.getInt("student_count"))
                        .instructorId(rs.getInt("instructor_id"))
                        .build()
        );
        return result.isEmpty() ? null : result.get(0);
    }

    // 🔎 강의실 검색 (제목 키워드로)
    public List<Classroom> findClassrooms(String keyword) {
        String sql = """
            SELECT course_id, instructor_id, title, category, status, student_count
            FROM courses
            WHERE deleted_at IS NULL
              AND title LIKE ?
            ORDER BY course_id DESC
        """;
        return jdbcTemplate.query(sql, new Object[]{"%" + keyword + "%"}, (rs, rowNum) -> Classroom.builder()
                .classroomId(rs.getInt("course_id"))
                .instructorId(rs.getInt("instructor_id"))
                .title(rs.getString("title"))
                .category(rs.getString("category"))
                .status(rs.getString("status"))
                .studentCount(rs.getInt("student_count"))
                .build()
        );
    }


    // 📋 특정 강의실의 수강생 조회
    public List<StudentDto> findStudentsByCourseId(int courseId) {
        String sql = """
         SELECT u.user_id,
               u.name,
               u.nickname,
               u.email,
               u.phone,
               u.address,
               u.birth_day,
               u.gender,
               u.status,
               u.created_at,
               e.enrolled_at
        FROM enrollments e
        JOIN users u ON e.student_id = u.user_id
        WHERE e.course_id = ?
        """;
        return jdbcTemplate.query(sql, new Object[]{courseId}, (rs, rowNum) -> StudentDto.builder()
                .userId(rs.getInt("user_id"))
                .name(rs.getString("name"))
                .nickname(rs.getString("nickname"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .address(rs.getString("address"))
                .birthDay(rs.getDate("birth_day") != null ? rs.getDate("birth_day").toLocalDate() : null)
                .gender(rs.getString("gender"))
                .status(rs.getString("status"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .enrolledAt(rs.getTimestamp("enrolled_at").toLocalDateTime())
                .build()
        );
    }


    // 단일 강의실 상태 업데이트
    public void updateStatus(Integer classroomId, String status) {
        String sql = "UPDATE courses SET status = ? WHERE course_id = ?";
        jdbcTemplate.update(sql, status, classroomId);
    }

    // 선택 삭제
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        String inSql = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
        String sql = "UPDATE courses SET deleted_at = NOW() WHERE course_id IN (" + inSql + ")";

        jdbcTemplate.update(sql, ids.toArray());
    }
}
