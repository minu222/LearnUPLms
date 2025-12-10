package com.lms.adminpages.email.dao;

import com.lms.adminpages.email.entity.Message;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("adminMessage")
public class MessageDao {

    private final JdbcTemplate jdbcTemplate;

    public MessageDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 메시지 보내기
    public int sendMessage(Message message) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, message.getSenderId(), message.getReceiverId(), message.getContent());
    }

    // 받은 메시지함
    public List<Message> getReceivedMessages(int receiverId) {
        String sql = "SELECT m.*, s.name AS senderName, r.name AS receiverName " +
                "FROM messages m " +
                "LEFT JOIN users s ON m.sender_id = s.user_id " +
                "LEFT JOIN users r ON m.receiver_id = r.user_id " +
                "WHERE m.receiver_id = ? ORDER BY m.sent_at DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Message.class), receiverId);
    }

    // 보낸 메시지함
    public List<Message> getSentMessages(int senderId) {
        String sql = "SELECT m.*, s.name AS senderName, r.name AS receiverName " +
                "FROM messages m " +
                "LEFT JOIN users s ON m.sender_id = s.user_id " +
                "LEFT JOIN users r ON m.receiver_id = r.user_id " +
                "WHERE m.sender_id = ? ORDER BY m.sent_at DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Message.class), senderId);
    }

    // 메시지 읽음 처리
    public int markAsRead(int messageId) {
        String sql = "UPDATE messages SET is_read = 1 WHERE message_id = ?";
        return jdbcTemplate.update(sql, messageId);
    }

    // 메시지 삭제
    public int deleteMessage(int messageId) {
        String sql = "DELETE FROM messages WHERE message_id = ?";
        return jdbcTemplate.update(sql, messageId);
    }

    // 메시지 상세
    public Message getMessageDetail(int messageId) {
        String sql = "SELECT m.*, s.name AS senderName, r.name AS receiverName " +
                "FROM messages m " +
                "LEFT JOIN users s ON m.sender_id = s.user_id " +
                "LEFT JOIN users r ON m.receiver_id = r.user_id " +
                "WHERE m.message_id = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Message.class), messageId);
    }
}