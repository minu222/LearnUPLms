package com.lms.mainpages.api;

import com.lms.mainpages.entity.User;
import com.lms.mainpages.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService service;

    public OrderApiController(OrderService service) {
        this.service = service;
    }

    // ===== 요청 DTO =====
    public static class PayRequest {
        public Long courseId;
        public String paymentMethod;
    }

    // ===== 결제 =====
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody PayRequest req, HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        if (req == null || req.courseId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "message", "courseId는 필수입니다."));
        }

        var r = service.payAndEnroll(login.getUser_id(), req.courseId, req.paymentMethod);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "orderId", r.orderId(),
                "paidAmount", r.paidAmount()
        ));
    }

    // ===== 내 주문 요약(first_title 포함) =====
    @GetMapping("/my")
    public ResponseEntity<?> myOrders(HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(service.findMyOrders(login.getUser_id()));
    }

    // ===== 409 매핑 =====
    @ExceptionHandler(OrderService.AlreadyEnrolledException.class)
    public ResponseEntity<?> handleDup(OrderService.AlreadyEnrolledException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("ok", false, "message", e.getMessage()));
    }
}
