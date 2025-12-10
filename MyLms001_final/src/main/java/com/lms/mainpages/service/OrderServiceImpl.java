package com.lms.mainpages.service;

import com.lms.mainpages.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orders;

    public OrderServiceImpl(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional
    public PayResult payAndEnroll(long userId, long courseId, String paymentMethod) {
        // 1) 이미 수강/이미 결제 방지
        if (orders.existsEnrollment(userId, courseId)) {
            throw new AlreadyEnrolledException("이미 수강 중인 강의입니다.");
        }
        if (orders.existsPaidByUserIdAndCourseId(userId, courseId)) {
            throw new AlreadyEnrolledException("이미 결제 완료된 강의입니다.");
        }

        // 2) 가격 확인 (무료면 0)
        Map<String, Object> row = orders.findCoursePriceRow(courseId);
        boolean isFree = toBool(row.get("is_free"));
        long price = toLong(row.get("price"));
        long amount = isFree ? 0L : price;

        // 3) 주문 생성(paid)
        long orderId = orders.insertPaidOrder(userId, amount, paymentMethod);

        // 4) 수강 등록 (enrollments)
        orders.insertEnrollment(orderId, userId, courseId);

        // 5) 장바구니 삭제
        orders.deleteCartItem(userId, courseId);

        return new PayResult(orderId, amount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findMyOrders(long userId) {
        return orders.listByUser(userId);
    }

    // helpers
    private static boolean toBool(Object v) {
        if (v == null) return false;
        String s = String.valueOf(v);
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }
    private static long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        if (v instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(String.valueOf(v));
    }
}
