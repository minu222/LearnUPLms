package com.lms.mainpages.board.free;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FreeBoardQueryRepository {

    private final JdbcTemplate jdbc;

    /* ======================================================================
       목록 집계
       - 카테고리는 DB 값(FREE/NOTICE 등)과 무관하게 LOWER()로 비교 → 대소문자 안전
       - keyword가 있으면 제목/내용 Like 검색
       ====================================================================== */
    public int countByCategory(String category, String keyword) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
              FROM project.posts p
             WHERE p.is_deleted = 0
               AND LOWER(p.category) = ?
            """);

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (p.title LIKE CONCAT('%',?,'%') OR p.content LIKE CONCAT('%',?,'%')) ");
            return jdbc.queryForObject(sql.toString(), Integer.class,
                    safeLower(category), keyword, keyword);
        }
        return jdbc.queryForObject(sql.toString(), Integer.class, safeLower(category));
    }

    /* ======================================================================
       페이지 목록
       - p.post_id → id 로 alias (뷰/JS에서 id로 통일해서 사용)
       - 작성자 이름/역할을 users에서 조인
       - 최신글 우선 정렬
       ====================================================================== */
    public List<PostListItem> findPageByCategory(String category, int page, int size, String keyword) {
        int pg   = Math.max(page, 1);
        int sz   = Math.max(size, 1);
        int off  = (pg - 1) * sz;

        StringBuilder sql = new StringBuilder("""
            SELECT
                   p.post_id   AS id,
                   p.title,
                   p.views,
                   p.created_at,
                   u.name      AS author_name,
                   u.role      AS author_role
              FROM project.posts p
              JOIN project.users u ON u.id = p.user_id
             WHERE p.is_deleted = 0
               AND LOWER(p.category) = ?
            """);

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (p.title LIKE CONCAT('%',?,'%') OR p.content LIKE CONCAT('%',?,'%')) ");
            sql.append(" ORDER BY p.post_id DESC LIMIT ? OFFSET ? ");
            return jdbc.query(sql.toString(), listRowMapper(),
                    safeLower(category), keyword, keyword, sz, off);
        } else {
            sql.append(" ORDER BY p.post_id DESC LIMIT ? OFFSET ? ");
            return jdbc.query(sql.toString(), listRowMapper(),
                    safeLower(category), sz, off);
        }
    }

    private RowMapper<PostListItem> listRowMapper() {
        return new RowMapper<>() {
            @Override
            public PostListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PostListItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("author_name"),
                        rs.getString("author_role"),
                        rs.getInt("views"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
            }
        };
    }

    /* ======================================================================
       상세 조회 (상세 화면/댓글 폼용)
       - category 조건을 넘길 수 있게 분리. 필요 없으면 null 전달.
       ====================================================================== */
    public Optional<PostDetail> findDetailById(long postId, String category) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                   p.post_id      AS id,
                   p.title,
                   p.content,
                   p.views,
                   p.likes,
                   p.comments_count,
                   p.created_at,
                   u.name         AS author_name,
                   u.role         AS author_role
              FROM project.posts p
              JOIN project.users u ON u.id = p.user_id
             WHERE p.is_deleted = 0
               AND p.post_id = ?
            """);

        Object[] args;
        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(p.category) = ? ");
            args = new Object[]{ postId, safeLower(category) };
        } else {
            args = new Object[]{ postId };
        }

        List<PostDetail> rows = jdbc.query(sql.toString(), detailRowMapper(), args);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private RowMapper<PostDetail> detailRowMapper() {
        return new RowMapper<>() {
            @Override
            public PostDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PostDetail(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("author_name"),
                        rs.getString("author_role"),
                        rs.getInt("views"),
                        rs.getInt("likes"),
                        rs.getInt("comments_count"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
            }
        };
    }

    private String safeLower(String s) {
        return (s == null) ? null : s.toLowerCase();
    }

    /* ===== 목록/상세 DTO ===== */
    public record PostListItem(
            long id,
            String title,
            String authorName,
            String authorRole,
            int views,
            LocalDateTime createdAt
    ) {}

    public record PostDetail(
            long id,
            String title,
            String content,
            String authorName,
            String authorRole,
            int views,
            int likes,
            int commentsCount,
            LocalDateTime createdAt
    ) {}
}
