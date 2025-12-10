package com.lms.adminpages.pay.service;

import com.lms.adminpages.pay.dao.OrderDao;
import com.lms.adminpages.pay.entity.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderDao orderDao;
    private final DataSource dataSource;


    @Autowired
    public OrderService(OrderDao orderDao, DataSource dataSource) {
        this.orderDao = orderDao;
        this.dataSource = dataSource;
    }


    public List<Order> getOrders(String keyword, String status) {
        return orderDao.searchOrders(keyword, status);
    }



    public List<Order> getPays(String keyword, String status) {
        List<Order> orders = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT o.*, u.name AS user_name " +
                        "FROM orders o LEFT JOIN users u ON o.user_id = u.user_id WHERE 1=1 "
        );

        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (o.order_id LIKE ? OR u.email LIKE ?) ");
        }
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append("AND o.status = ? ");
        }

        sql.append("ORDER BY o.created_at DESC");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int index = 1;
            if (keyword != null && !keyword.isEmpty()) {
                pstmt.setString(index++, "%" + keyword + "%");
                pstmt.setString(index++, "%" + keyword + "%");
            }
            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                pstmt.setString(index++, status);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = Order.builder()
                            .orderId(rs.getInt("order_id"))
                            .userId(rs.getInt("user_id"))
                            .totalAmount(rs.getBigDecimal("total_amount"))
                            .status(rs.getString("status"))
                            .paymentMethod(rs.getString("payment_method"))
                            .createdAt(rs.getTimestamp("created_at"))
                            .refundedAt(rs.getTimestamp("refunded_at"))
                            .refundReason(rs.getString("refund_reason"))
                            .userName(rs.getString("user_name"))
                            .build();
                    orders.add(order);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

}