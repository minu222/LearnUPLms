package com.lms.adminpages.pay.dao;

import com.lms.adminpages.pay.entity.Order;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderDao {

    private final JdbcTemplate jdbcTemplate;

    public OrderDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Order> searchOrders(String keyword, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT o.order_id, o.user_id, o.total_amount, o.status, o.payment_method,
                   o.created_at, o.refunded_at, o.refund_reason,
                   u.name AS user_name,
                   '자바 과정' AS product_name
            FROM orders o
            LEFT JOIN users u ON o.user_id = u.user_id
            WHERE 1=1
            """);

        // 검색 조건 추가
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (CAST(o.order_id AS CHAR) LIKE ? OR u.name LIKE ?) ");
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND o.status = ? ");
        }

        // 파라미터 구성
        Object[] params;
        if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
            params = new Object[]{"%" + keyword + "%", "%" + keyword + "%", status};
        } else if (keyword != null && !keyword.isEmpty()) {
            params = new Object[]{"%" + keyword + "%", "%" + keyword + "%"};
        } else if (status != null && !status.isEmpty()) {
            params = new Object[]{status};
        } else {
            params = new Object[]{};
        }

        return jdbcTemplate.query(
                sql.toString(),
                params,
                new BeanPropertyRowMapper<>(Order.class)
        );
    }
}