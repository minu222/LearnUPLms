package com.lms.mainpages.repository;

import com.lms.mainpages.entity.User;
import com.lms.mainpages.exceptoin.DuplicateFieldException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /* ========================= Enum 변환 헬퍼 (대소문자 무시) ========================= */
    private static User.Role toRole(String s) {
        if (s == null) return null;
        try { return User.Role.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private static User.Gender toGender(String s) {
        if (s == null) return null;
        try { return User.Gender.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private static User.Status toStatus(String s) {
        if (s == null) return null;
        try { return User.Status.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return null; }
    }

    /* ========================= 공용 RowMapper ========================= */
    private static final RowMapper<User> USER_MAPPER = (rs, n) -> {
        User u = new User();
        u.setUser_id(rs.getInt("user_id"));
        u.setNickname(rs.getString("nickname"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setName(rs.getString("name"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setRole(toRole(rs.getString("role")));
        u.setGender(toGender(rs.getString("gender")));
        u.setStatus(toStatus(rs.getString("status")));
        Date bd = rs.getDate("birth_day");
        if (bd != null) u.setBirth_day(bd.toLocalDate());
        u.setCreated_at(rs.getTimestamp("created_at"));
        u.setUpdated_at(rs.getTimestamp("updated_at"));
        return u;
    };

    /* ========================= CREATE ========================= */

    /** 저장 + 생성된 PK 반환 (중복키를 DuplicateFieldException으로 변환) */
    public long saveAndReturnId(User user) {
        final String sql = """
            INSERT INTO users
              (nickname, email, password, name, phone, address,
               role, birth_day, gender, status, created_at, updated_at)
            VALUES
              (?, ?, ?, ?, ?, ?,
               ?, ?, ?, ?, NOW(), NOW())
            """;

        KeyHolder kh = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                ps.setString(i++, user.getNickname());
                ps.setString(i++, user.getEmail());
                ps.setString(i++, user.getPassword()); // ⚠ 운영 시 반드시 해시 처리
                ps.setString(i++, user.getName());
                ps.setString(i++, user.getPhone());
                ps.setString(i++, user.getAddress());
                ps.setString(i++, user.getRole() != null ? user.getRole().name() : null);

                LocalDate birth = user.getBirth_day();
                if (birth != null) ps.setDate(i++, Date.valueOf(birth)); else ps.setNull(i++, Types.DATE);

                ps.setString(i++, user.getGender() != null ? user.getGender().name() : null);
                ps.setString(i++, user.getStatus() != null ? user.getStatus().name() : User.Status.ACTIVE.name());
                return ps;
            }, kh);
        } catch (DuplicateKeyException e) {
            throw mapDuplicateToField(e, user);
        }

        Number key = kh.getKey();
        if (key == null) throw new IllegalStateException("user_id 생성 실패");
        user.setUser_id(key.intValue());
        return key.longValue();
    }

    public User save(User user) { saveAndReturnId(user); return user; }
    public void add(User user)  { save(user); }

    /* ========================= READ ========================= */

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users", USER_MAPPER);
    }

    /** 로그인/조회: soft delete 대신 status 필터 사용 */
    public Optional<User> findByNickname(String nickname) {
        String sql = """
            SELECT * FROM users
            WHERE nickname = ?
              AND (status IS NULL OR UPPER(status) <> 'DELETED')
            LIMIT 1
            """;
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, USER_MAPPER, nickname));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(int userId) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject("SELECT * FROM users WHERE user_id = ?", USER_MAPPER, userId)
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /* ========================= UPDATE ========================= */

    /** 일부 필드만 간단 수정 (이메일 중복은 폼 에러로 변환) */
    public int updateBasics(User user) {
        String sql = """
            UPDATE users
               SET address = ?, email = ?, phone = ?, updated_at = NOW()
             WHERE user_id = ?
            """;
        try {
            return jdbc.update(sql, user.getAddress(), user.getEmail(), user.getPhone(), user.getUser_id());
        } catch (DuplicateKeyException e) {
            throw mapDuplicateToField(e, user);
        }
    }

    /**
     * 프로필 전체 수정(+비밀번호 선택적 변경)
     * ⚠ 요구사항: 닉네임은 업데이트 금지!
     * - 시그니처는 유지하지만 nickname 파라미터는 **무시**합니다.
     */
    public int updateProfileAllFields(int userId,
                                      String nickname /*ignored*/,
                                      String name,
                                      String email,
                                      String phone,
                                      String address,
                                      String passwordOrNull) {
        StringBuilder sb = new StringBuilder("""
            UPDATE users
               SET name = ?, email = ?, phone = ?, address = ?
            """);
        boolean changePw = passwordOrNull != null && !passwordOrNull.isBlank();
        if (changePw) sb.append(", password = ?");
        sb.append(", updated_at = NOW() WHERE user_id = ?");

        try {
            return jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sb.toString());
                int i = 1;
                ps.setString(i++, name);
                ps.setString(i++, email);
                ps.setString(i++, phone);
                ps.setString(i++, address);
                if (changePw) ps.setString(i++, passwordOrNull); // ⚠ 운영 시 해시
                ps.setInt(i++, userId);
                return ps;
            });
        } catch (DuplicateKeyException e) {
            // 이메일 유니크 충돌을 DuplicateFieldException("email")로 변환
            throw mapDuplicateToField(e, new User() {{ setEmail(email); setNickname(nickname); }});
        }
    }

    /* ========================= DELETE ========================= */

    /** 소프트 삭제: status='DELETED' */
    public int deleteById(int userId) {
        return jdbc.update("UPDATE users SET status='DELETED', updated_at=NOW() WHERE user_id=?", userId);
    }

    /* ========================= 부가 기능 ========================= */

    /** 닉네임 중복(다른 사용자 제외) */
    public boolean existsByNicknameExceptId(String nickname, int exceptUserId) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE nickname = ? AND user_id <> ?",
                Integer.class, nickname, exceptUserId
        );
        return cnt != null && cnt > 0;
    }

    /** 닉네임 중복 */
    public boolean existsByNickname(String nickname) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE nickname = ?",
                Integer.class, nickname
        );
        return cnt != null && cnt > 0;
    }

    /** 이메일 중복 */
    public boolean existsByEmail(String email) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class, email
        );
        return cnt != null && cnt > 0;
    }

    /** 이메일 중복(본인 제외) */
    public boolean existsByEmailExceptId(String email, int exceptUserId) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND user_id <> ?",
                Integer.class, email, exceptUserId
        );
        return cnt != null && cnt > 0;
    }

    /** 강사 프로필 저장 – 테이블 컬럼명이 'instructor_id' 여야 합니다! */
    public int insertInstructorProfile(int userId, String affiliation, String bio) {
        String sql = """
            INSERT INTO instructor_profile (instructor_id, affiliation, bio)
            VALUES (?, ?, ?)
            """;
        return jdbc.update(sql, userId, affiliation, bio);
    }

    /** 옛 서비스 호환용 래퍼 */
    public long insertUser(User u) {
        return saveAndReturnId(u);
    }

    /* ========================= 내부 유틸: 중복키 → 필드 예외 매핑 ========================= */

    private RuntimeException mapDuplicateToField(DuplicateKeyException e, User uCtx) {
        String msg = String.valueOf(
                e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()
        ).toLowerCase();

        // 인덱스/키 이름은 환경마다 다를 수 있어 contains 조건을 넉넉히 둡니다.
        if (msg.contains("users.email") || msg.contains("for key 'email'") || msg.contains("uk_email")) {
            return new DuplicateFieldException("email", uCtx != null ? uCtx.getEmail() : null);
        }
        if (msg.contains("users.nickname") || msg.contains("for key 'nickname'") || msg.contains("uk_nickname")) {
            return new DuplicateFieldException("nickname", uCtx != null ? uCtx.getNickname() : null);
        }
        return e; // 다른 제약은 원래 예외 전달
    }
}
