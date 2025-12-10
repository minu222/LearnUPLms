package com.lms.adminpages.pay.entity;

import lombok.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private Integer orderId;
    private Integer userId;
    private BigDecimal totalAmount;
    private String status;          // paid, pending, refund_requested, refunded
    private String paymentMethod;
    private Timestamp createdAt;
    private Timestamp refundedAt;
    private String refundReason;

    // JOIN 결과로 추가되는 필드
    private String userName;     // 고객명 (users.name)
    private String productName;  // 상품명 (임시 or 추후 order_items 조인)
}