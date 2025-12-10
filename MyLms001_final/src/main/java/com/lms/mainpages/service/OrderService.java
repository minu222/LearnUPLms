package com.lms.mainpages.service;

import java.util.List;
import java.util.Map;

public interface OrderService {

    record PayResult(Long orderId, long paidAmount) {}

    class AlreadyEnrolledException extends RuntimeException {
        public AlreadyEnrolledException(String message) { super(message); }
    }

    PayResult payAndEnroll(long userId, long courseId, String paymentMethod);

    List<Map<String, Object>> findMyOrders(long userId);
}
