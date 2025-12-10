package com.lms.mainpages.repository;

import com.lms.mainpages.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

@Repository("mainMessage")
@RequiredArgsConstructor
public class MessageDao {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Message> MAPPER = (rs, i) -> Message.builder()
            .id(rs.getLong("message_id"))
            .senderId(rs.getLong("sender_id"))
            .receiverId(rs.getLong("receiver_id"))
            .senderNickname(rs.getString("sender_nickname"))
            .receiverNickname(rs.getString("receiver_nickname"))
            .senderName(rs.getString("sender_name"))
            .receiverName(rs.getString("receiver_name"))
            .senderRole(rs.getString("sender_role"))
            .receiverRole(rs.getString("receiver_role"))
            .content(rs.getString("content"))
            .read(rs.getBoolean("is_read"))
            .sentAt(toLdt(rs.getTimestamp("sent_at")))
            .build();

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    public List<Message> findInbox(Long userId) {
        String sql = """
            SELECT m.message_id, m.sender_id, m.receiver_id, m.content, m.is_read, m.sent_at,
                   s.nickname AS sender_nickname, s.name AS sender_name, s.role AS sender_role,
                   r.nickname AS receiver_nickname, r.name AS receiver_name, r.role AS receiver_role
            FROM messages m
            LEFT JOIN users s ON s.user_id = m.sender_id
            LEFT JOIN users r ON r.user_id = m.receiver_id
            WHERE m.receiver_id = ?
            ORDER BY m.sent_at DESC
        """;
        return jdbc.query(sql, MAPPER, userId);
    }

    public List<Message> findSent(Long userId) {
        String sql = """
            SELECT m.message_id, m.sender_id, m.receiver_id, m.content, m.is_read, m.sent_at,
                   s.nickname AS sender_nickname, s.name AS sender_name, s.role AS sender_role,
                   r.nickname AS receiver_nickname, r.name AS receiver_name, r.role AS receiver_role
            FROM messages m
            LEFT JOIN users s ON s.user_id = m.sender_id
            LEFT JOIN users r ON r.user_id = m.receiver_id
            WHERE m.sender_id = ?
            ORDER BY m.sent_at DESC
        """;
        return jdbc.query(sql, MAPPER, userId);
    }

    public Long save(Long senderId, Long receiverId, String content) {
        String sql = """
            INSERT INTO messages (sender_id, receiver_id, content, is_read, sent_at)
            VALUES (?, ?, ?, 0, NOW())
        """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, senderId);
            ps.setLong(2, receiverId);
            ps.setString(3, content);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public void markRead(Long id, boolean read) {
        jdbc.update("UPDATE messages SET is_read=? WHERE message_id=?", read, id);
    }

    public void delete(Long id) {
        jdbc.update("DELETE FROM messages WHERE message_id=?", id);
    }

    /** 닉네임 → user_id (없으면 null) */
    public Long findUserIdByNickname(String nickname) {
        try {
            return jdbc.queryForObject(
                    "SELECT user_id FROM users WHERE nickname = ?",
                    Long.class, nickname
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
