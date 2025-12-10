package com.lms.mainpages.service;

import com.lms.mainpages.api.dto.PostSummaryDto;
import com.lms.mainpages.repository.PostSchemaResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class MyPostsJdbcService {

    private final JdbcTemplate jdbc;
    private final PostSchemaResolver schema;

    public MyPostsJdbcService(JdbcTemplate jdbc, PostSchemaResolver schema) {
        this.jdbc = jdbc;
        this.schema = schema;
    }

    public List<PostSummaryDto> list(Long userId) {
        var s = schema.get();
        String createdSel  = s.created  != null ? s.created  : "NULL";
        String categorySel = s.category != null ? s.category : "NULL";
        String orderBy     = s.created  != null ? s.created  : s.id;

        String sql = "SELECT " + s.id + " AS id, "
                + s.title   + " AS title, "
                + s.content + " AS content, "
                + categorySel + " AS category, "
                + createdSel  + " AS created_at "
                + "FROM " + s.table + " WHERE " + s.author + " = ? "
                + "ORDER BY " + orderBy + " DESC";

        return jdbc.query(sql, (rs, i) -> {
            PostSummaryDto dto = new PostSummaryDto();
            dto.setId(rs.getLong("id"));
            dto.setTitle(rs.getString("title"));
            dto.setContent(rs.getString("content"));
            dto.setCategory(rs.getString("category"));
            Timestamp ts = rs.getTimestamp("created_at");
            dto.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
            return dto;
        }, userId);
    }

    public PostSummaryDto one(Long id, Long userId) {
        var s = schema.get();
        String createdSel  = s.created  != null ? s.created  : "NULL";
        String categorySel = s.category != null ? s.category : "NULL";

        String sql = "SELECT " + s.id + " AS id, "
                + s.title   + " AS title, "
                + s.content + " AS content, "
                + categorySel + " AS category, "
                + createdSel  + " AS created_at "
                + "FROM " + s.table + " WHERE " + s.id + " = ? AND " + s.author + " = ?";

        List<PostSummaryDto> list = jdbc.query(sql, (rs, i) -> {
            PostSummaryDto dto = new PostSummaryDto();
            dto.setId(rs.getLong("id"));
            dto.setTitle(rs.getString("title"));
            dto.setContent(rs.getString("content"));
            dto.setCategory(rs.getString("category"));
            Timestamp ts = rs.getTimestamp("created_at");
            dto.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
            return dto;
        }, id, userId);

        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public int update(Long id, Long userId, String title, String content) {
        var s = schema.get();
        String sql = "UPDATE " + s.table + " SET " + s.title + " = ?, " + s.content + " = ? "
                + "WHERE " + s.id + " = ? AND " + s.author + " = ?";
        return jdbc.update(sql, title, content, id, userId);
    }

    @Transactional
    public int delete(Long id, Long userId) {
        var s = schema.get();
        String sql = "DELETE FROM " + s.table + " WHERE " + s.id + " = ? AND " + s.author + " = ?";
        return jdbc.update(sql, id, userId);
    }
}

