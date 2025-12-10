package com.lms.mainpages.board.post.repository;

import com.lms.mainpages.board.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostJdbcRepository implements PostRepository {

    private final JdbcTemplate jdbc;

    private static java.time.LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    // ✅ DB: project.posts (복수형), 컬럼: post_id, user_id, title, content, category, views, likes,
    //    comments_count, is_deleted, created_at, updated_at
    private final RowMapper<Post> rowMapper = (rs, n) -> Post.builder()
            .id(rs.getLong("id"))                         // alias 사용 (post_id AS id)
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .category(rs.getString("category"))
            .createdAt(toLocal(rs.getTimestamp("created_at")))
            .updatedAt(toLocal(rs.getTimestamp("updated_at")))
            .build();

    @Override
    public List<Post> findPage(String category, String q, int page, int size) {
        int offset = page * size;

        String base = """
            SELECT
              post_id AS id,
              title, content, category, created_at, updated_at
            FROM posts
            WHERE category = ? AND is_deleted = 0
            """;

        String orderLimit = " ORDER BY post_id DESC LIMIT ? OFFSET ?";

        if (q == null || q.isBlank()) {
            return jdbc.query(base + orderLimit, rowMapper, category, size, offset);
        }

        String withSearch = base + " AND title LIKE ? " + orderLimit;
        return jdbc.query(withSearch, rowMapper, category, "%" + q.trim() + "%", size, offset);
    }

    @Override
    public long count(String category, String q) {
        String base = "SELECT COUNT(*) FROM posts WHERE category = ? AND is_deleted = 0";
        if (q == null || q.isBlank()) {
            return jdbc.queryForObject(base, Long.class, category);
        }
        String withSearch = base + " AND title LIKE ?";
        return jdbc.queryForObject(withSearch, Long.class, category, "%" + q.trim() + "%");
    }

    @Override
    public Post findById(Long id) {
        try {
            return jdbc.queryForObject("""
                SELECT
                  post_id AS id,
                  title, content, category, created_at, updated_at
                FROM posts
                WHERE post_id = ? AND is_deleted = 0
                """, rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
