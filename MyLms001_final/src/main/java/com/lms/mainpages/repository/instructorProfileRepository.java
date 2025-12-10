package com.lms.mainpages.repository;

import com.lms.mainpages.entity.instructorProfile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class instructorProfileRepository {

    private final JdbcTemplate jdbc;

    public instructorProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** users.user_id = instructor_profile.instructor_id 라고 가정 */
    public Optional<instructorProfile> findByUserId(int userId) {
        final String sql =
                "SELECT instructor_id, affiliation, bio " +
                        "FROM instructor_profile " +
                        "WHERE instructor_id = ?";

        try {
            instructorProfile p = jdbc.queryForObject(sql, (rs, rn) ->
                            // 엔티티의 (int, String, String) 생성자 사용
                            new instructorProfile(
                                    rs.getInt("instructor_id"),
                                    rs.getString("affiliation"),
                                    rs.getString("bio")
                            ),
                    userId
            );
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** 존재하면 UPDATE, 없으면 INSERT (경합 시 중복키 예외 처리) */
    public int upsert(long userId, String affiliation, String bio) {
        // 1) UPDATE 시도
        int rows = jdbc.update(
                "UPDATE instructor_profile SET affiliation = ?, bio = ? WHERE instructor_id = ?",
                affiliation, bio, userId
        );
        if (rows > 0) return rows;

        // 2) 없으면 INSERT
        try {
            rows = jdbc.update(
                    "INSERT INTO instructor_profile (instructor_id, affiliation, bio) VALUES (?, ?, ?)",
                    userId, affiliation, bio
            );
            return rows;
        } catch (DuplicateKeyException ex) {
            // 3) 경합으로 이미 누군가 INSERT 했다면 UPDATE 재시도
            return jdbc.update(
                    "UPDATE instructor_profile SET affiliation = ?, bio = ? WHERE instructor_id = ?",
                    affiliation, bio, userId
            );
        }
    }

    public int deleteByUserId(int userId) {
        return jdbc.update("DELETE FROM instructor_profile WHERE instructor_id = ?", userId);
    }
}