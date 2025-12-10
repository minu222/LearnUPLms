package com.lms.adminpages.pay.controller;

import com.lms.adminpages.pay.dao.OrderDao;
import com.lms.adminpages.pay.entity.Order;
import com.lms.adminpages.pay.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderListController {


    private final OrderService orderService;

    @GetMapping("/order-list")
    public String getOrders(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            Model model) {

        List<Order> orders = orderService.getOrders(keyword, status);

        model.addAttribute("orders", orders);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);


        return "adminpages/order-list/index";
    }
}
