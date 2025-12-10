package com.lms.adminpages.dashboard.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationDao{

    private final JdbcTemplate jdbcTemplate;

    public NotificationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 최근 알림 3개 가져오기
    public List<String> getRecentNotifications() {
        String sql = "SELECT content FROM notifications ORDER BY created_at DESC LIMIT 3";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}