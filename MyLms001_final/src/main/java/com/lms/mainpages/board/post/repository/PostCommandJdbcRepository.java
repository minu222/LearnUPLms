package com.lms.mainpages.board.post.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

@Repository
@RequiredArgsConstructor
public class PostCommandJdbcRepository implements PostCommandRepository {

    private final JdbcTemplate jdbc;

    @Override
    public Long insert(String title, String content, String category, Long userId) {
        String sql = """
            INSERT INTO posts
              (user_id, title, content, category, views, likes, comments_count, is_deleted, created_at, updated_at)
            VALUES
              (?, ?, ?, ?, 0, 0, 0, 0, NOW(6), NOW(6))
            """;

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"post_id"});
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);   // HTML 저장 (서버단에서 필요시 sanitizing)
            ps.setString(4, category);  // "free" | "NOTICE"
            return ps;
        }, kh);

        Number key = kh.getKey();
        return (key == null) ? null : key.longValue();
    }
}

