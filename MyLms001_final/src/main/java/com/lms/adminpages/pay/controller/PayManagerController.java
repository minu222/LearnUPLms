package com.lms.adminpages.pay.controller;

import com.lms.adminpages.pay.entity.Order;
import com.lms.adminpages.pay.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/order")
public class PayManagerController {

    private final OrderService orderService;

    public PayManagerController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/payment-management")
    public String listOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<Order> orders = orderService.getPays(keyword, status);
        model.addAttribute("orders", orders);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "adminpages/payment-management/index";
    }
}
