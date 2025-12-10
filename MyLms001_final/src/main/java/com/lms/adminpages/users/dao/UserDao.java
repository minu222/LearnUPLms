package com.lms.adminpages.users.dao;

import com.lms.adminpages.users.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserDao {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findInstructors(String status, String department, String keywordType, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE role = 'instructor'");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        if (department != null && !department.isEmpty()) {
            sql.append(" AND address = ?");
            params.add(department);
        }

        if (keyword != null && !keyword.isEmpty()) {
            if ("name".equals(keywordType)) {
                sql.append(" AND name LIKE ?");
                params.add("%" + keyword + "%");
            } else if ("nickname".equals(keywordType)) {
                sql.append(" AND nickname LIKE ?");
                params.add("%" + keyword + "%");
            } else {
                sql.append(" AND (name LIKE ? OR nickname LIKE ?)");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), new UserRowMapper());
    }

    public User findById(Integer userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, new Object[]{userId}, new BeanPropertyRowMapper<>(User.class));
        return users.isEmpty() ? null : users.get(0);


    }

    // =============================
    // 동적 필터 적용 가능한 학생 조회
    // =============================
    public List<User> findStudents(String status, String department, String keywordType, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE role = 'student'");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        if (department != null && !department.isEmpty()) {
            sql.append(" AND address = ?");
            params.add(department);
        }

        if (keyword != null && !keyword.isEmpty()) {
            if ("name".equals(keywordType)) {
                sql.append(" AND name LIKE ?");
                params.add("%" + keyword + "%");
            } else if ("nickname".equals(keywordType)) {
                sql.append(" AND nickname LIKE ?");
                params.add("%" + keyword + "%");
            } else {
                sql.append(" AND (name LIKE ? OR nickname LIKE ?)");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), new UserRowMapper());
    }



    // 공통 RowMapper
    public class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUser_id(rs.getInt("user_id"));
            user.setNickname(rs.getString("nickname"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setName(rs.getString("name"));
            user.setPhone(rs.getString("phone"));
            user.setAddress(rs.getString("address"));
            user.setBirth_day(rs.getDate("birth_day") != null ? rs.getDate("birth_day").toLocalDate() : null);
            user.setGender(rs.getString("gender") != null ? User.Gender.valueOf(rs.getString("gender").toLowerCase()) : null);
            user.setEmail_verified(rs.getBoolean("email_verified"));

            // Role
            String roleStr = rs.getString("role");
            if (roleStr != null) {
                user.setRole(User.Role.valueOf(roleStr.toLowerCase()));
            }

            // Status
            String statusStr = rs.getString("status");
            if (statusStr != null) {
                user.setStatus(User.Status.valueOf(statusStr.toLowerCase()));
            }

            user.setCreated_at(rs.getTimestamp("created_at") != null ?
                    rs.getTimestamp("created_at").toLocalDateTime() : null);

            user.setUpdated_at(rs.getTimestamp("updated_at") != null ?
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);

            return user;
        }
    }

    public void updateStatusToDeleted(int[] ids) {
        String sql = "UPDATE users SET status = 'deleted' WHERE user_id = ?";
        for (int id : ids) {
            jdbcTemplate.update(sql, id);
        }
    }



    // 모든 회원 조회
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY user_id DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = new User();
            user.setUser_id(rs.getInt("user_id"));
            user.setNickname(rs.getString("nickname"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setName(rs.getString("name"));
            user.setPhone(rs.getString("phone"));
            user.setAddress(rs.getString("address"));
            user.setRole(User.Role.valueOf(rs.getString("role")));
            Date birthDay = rs.getDate("birth_day");
            user.setBirth_day(birthDay != null ? birthDay.toLocalDate() : null);
            user.setGender(User.Gender.valueOf(rs.getString("gender")));
            user.setStatus(User.Status.valueOf(rs.getString("status")));
            user.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
            user.setUpdated_at(rs.getTimestamp("updated_at").toLocalDateTime());
            return user;
        });
    }

    public List<User> findByFilter(String group, String type, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 그룹 필터 (role)
        if (group != null && !group.isEmpty()) {
            sql.append(" AND role = ?");
            params.add(group.equals("instructor") ? "instructor" : "student");
        }

        // 검색 필터
        if (type != null && !type.isEmpty() && keyword != null && !keyword.isEmpty()) {
            switch (type) {
                case "name":
                    sql.append(" AND name LIKE ?");
                    break;
                case "username":
                    sql.append(" AND nickname LIKE ?");  // 엔티티 nickname 기준
                    break;
                case "email":
                    sql.append(" AND email LIKE ?");
                    break;
            }
            params.add("%" + keyword + "%");
        }

        sql.append(" ORDER BY user_id DESC");

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                new BeanPropertyRowMapper<>(User.class)
        );
    }


    // 회원 목록 (탈퇴 제외)
    public List<User> findActiveMembers(String group, String type, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE status <> 'deleted'");
        List<Object> params = new ArrayList<>();

        // 그룹 필터
        if (group != null && !group.isEmpty()) {
            sql.append(" AND role = ?");
            params.add(group);
        }

        // 검색 필터
        if (type != null && keyword != null && !keyword.isEmpty()) {
            switch (type) {
                case "name":
                    sql.append(" AND name LIKE ?");
                    break;
                case "nickname":  // username 대신 nickname 사용
                    sql.append(" AND nickname LIKE ?");
                    break;
                case "email":
                    sql.append(" AND email LIKE ?");
                    break;
                default:
                    throw new IllegalArgumentException("알 수 없는 검색 타입: " + type);
            }
            params.add("%" + keyword + "%");
        }

        sql.append(" ORDER BY created_at DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            User u = new User();
            u.setUser_id(rs.getInt("user_id"));
            u.setNickname(rs.getString("nickname"));
            u.setName(rs.getString("name"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setStatus(User.Status.valueOf(rs.getString("status")));
            u.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
            return u;
        });
    }

    //선택삭제
    public void softDeleteById(int id) {
        String sql = "UPDATE users SET status = 'deleted', updated_at = NOW() WHERE user_id = ?";
        jdbcTemplate.update(sql, id);
    }

    //휴지통
    public List<User> findDeletedUsers() {
        String sql = "SELECT * FROM users WHERE status = 'deleted' ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User u = new User();
            u.setUser_id(rs.getInt("user_id"));
            u.setNickname(rs.getString("nickname"));
            u.setName(rs.getString("name"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setStatus(User.Status.valueOf(rs.getString("status")));
            u.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
            u.setUpdated_at(rs.getTimestamp("updated_at").toLocalDateTime());
            return u;
        });
    }


    // ✅ 선택 복원 (status -> active)
    public void restoreUsers(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        String sql = "UPDATE users SET status = 'active' WHERE user_id IN (" +
                ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        jdbcTemplate.update(sql, ids.toArray());
    }
}
