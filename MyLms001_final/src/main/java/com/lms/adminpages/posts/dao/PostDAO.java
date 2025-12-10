package com.lms.adminpages.posts.dao;

import com.lms.adminpages.posts.entity.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class PostDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 전체 조회 + 검색 필터
    public List<Post> searchPosts(String category, String state, String type, String keyword) {
        String sql = """
            SELECT p.post_id, p.user_id, p.title, p.content, p.category, p.views, p.likes, p.comments_count,
                   p.is_deleted, p.created_at, p.updated_at, u.nickname AS authorName
            FROM posts p
            LEFT JOIN users u ON p.user_id = u.user_id
            WHERE p.is_deleted = 0
        """;

        List<Object> params = new ArrayList<>();

        if (category != null && !category.isEmpty()) {
            sql += " AND p.category = ?";
            params.add(category);
        }

        if (type != null && keyword != null && !keyword.isEmpty()) {
            if (type.equals("title")) {
                sql += " AND p.title LIKE ?";
                params.add("%" + keyword + "%");
            } else if (type.equals("author")) {
                sql += " AND u.nickname LIKE ?";
                params.add("%" + keyword + "%");
            }
        }

        sql += " ORDER BY p.created_at DESC";

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> Post.builder()
                .postId(rs.getInt("post_id"))
                .userId(rs.getInt("user_id"))
                .title(rs.getString("title"))
                .content(rs.getString("content"))
                .category(rs.getString("category"))
                .views(rs.getInt("views"))
                .likes(rs.getInt("likes"))
                .commentsCount(rs.getInt("comments_count"))
                .isDeleted(rs.getBoolean("is_deleted"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .authorName(rs.getString("authorName"))
                .build()
        );
    }

    // 상세 조회
    public Post findById(Integer postId) {
        String sql = """
            SELECT p.post_id, p.user_id, p.title, p.content, p.category, p.views, p.likes, p.comments_count,
                   p.is_deleted, p.created_at, p.updated_at, u.nickname AS authorName
            FROM posts p
            LEFT JOIN users u ON p.user_id = u.user_id
            WHERE p.post_id = ? AND p.is_deleted = 0
        """;

        return jdbcTemplate.queryForObject(sql, new Object[]{postId}, (rs, rowNum) -> Post.builder()
                .postId(rs.getInt("post_id"))
                .userId(rs.getInt("user_id"))
                .title(rs.getString("title"))
                .content(rs.getString("content"))
                .category(rs.getString("category"))
                .views(rs.getInt("views"))
                .likes(rs.getInt("likes"))
                .commentsCount(rs.getInt("comments_count"))
                .isDeleted(rs.getBoolean("is_deleted"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .authorName(rs.getString("authorName"))
                .build()
        );
    }

    // 선택 삭제 (is_deleted = 1)
    public void markAsDeleted(List<Integer> postIds) {
        String sql = "UPDATE posts SET is_deleted = 1 WHERE post_id IN (:ids)";
        Map<String, Object> paramMap = Collections.singletonMap("ids", postIds);
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        namedJdbcTemplate.update(sql, paramMap);
    }


    public void save(Post post) {
        String sql = """
            INSERT INTO posts (user_id, title, content, category, views, likes, comments_count, is_deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(sql,
                post.getUserId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getViews(),
                post.getLikes(),
                post.getCommentsCount(),
                post.getIsDeleted(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}