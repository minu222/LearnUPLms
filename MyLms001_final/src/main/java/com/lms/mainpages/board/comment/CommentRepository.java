// src/main/java/dwacademy/mylms001/board/comment/CommentRepository.java
package com.lms.mainpages.board.comment;

import com.lms.mainpages.board.comment.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final JdbcTemplate jdbc;

    private static CommentDto mapRow(ResultSet rs) throws SQLException {
        return CommentDto.builder()
                .id(rs.getLong("comment_id"))
                .postId(rs.getLong("post_id"))
                .userId(rs.getLong("user_id"))
                .content(rs.getString("content"))
                .likes((Integer) rs.getObject("likes"))
                .parentCommentId((Long) rs.getObject("parent_comment_id"))
                .deleted(rs.getInt("is_deleted") == 1)
                .createdAt(toLocal(rs.getTimestamp("created_at")))
                .updatedAt(toLocal(rs.getTimestamp("updated_at")))
                .authorName(rs.getString("author_name"))
                .authorRole(rs.getString("author_role"))
                .build();
    }

    private static java.time.LocalDateTime toLocal(Timestamp ts) {
        return (ts == null) ? null : ts.toLocalDateTime();
    }

    /** 해당 게시글의 전체 댓글(루트 → 자식 1단계) 시간순 */
    public List<CommentDto> findByPostId(long postId) {
        String sql = """
            SELECT  c.comment_id, c.post_id, c.user_id, c.content, c.likes, c.parent_comment_id,
                    c.is_deleted, c.created_at, c.updated_at,
                    u.name AS author_name, u.role AS author_role
              FROM project.comments c
         LEFT JOIN project.users u ON u.user_id = c.user_id
             WHERE c.post_id = ? AND c.is_deleted = 0
             ORDER BY COALESCE(c.parent_comment_id, c.comment_id), c.created_at ASC
        """;
        return jdbc.query(sql, (rs, rn) -> mapRow(rs), postId);
    }

    /** 단건 조회 */
    public CommentDto findById(long commentId) {
        String sql = """
            SELECT  c.comment_id, c.post_id, c.user_id, c.content, c.likes, c.parent_comment_id,
                    c.is_deleted, c.created_at, c.updated_at,
                    u.name AS author_name, u.role AS author_role
              FROM project.comments c
         LEFT JOIN project.users u ON u.user_id = c.user_id
             WHERE c.comment_id = ?
        """;
        return jdbc.query(sql, rs -> rs.next() ? mapRow(rs) : null, commentId);
    }

    /** 등록 (parentCommentId 없으면 NULL) */
    public long insert(long postId, long userId, String content, Long parentCommentId) {
        String sql = """
            INSERT INTO project.comments
                (post_id, user_id, content, likes, parent_comment_id, is_deleted, created_at, updated_at)
            VALUES (?, ?, ?, NULL, ?, 0, NOW(), NOW())
        """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            ps.setString(3, content);
            if (parentCommentId == null) ps.setNull(4, Types.BIGINT);
            else ps.setLong(4, parentCommentId);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return (key == null) ? -1L : key.longValue();
    }

    /** 내용 수정 (작성자만) */
    public int updateContent(long commentId, long userId, String content) {
        String sql = """
            UPDATE project.comments
               SET content = ?, updated_at = NOW()
             WHERE comment_id = ? AND user_id = ? AND is_deleted = 0
        """;
        return jdbc.update(sql, content, commentId, userId);
    }

    /** 소프트 삭제 (작성자만) */
    public int softDelete(long commentId, long userId) {
        String sql = """
            UPDATE project.comments
               SET is_deleted = 1, updated_at = NOW()
             WHERE comment_id = ? AND user_id = ? AND is_deleted = 0
        """;
        return jdbc.update(sql, commentId, userId);
    }
}
