package com.lms.adminpages.pay.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/order")
public class CouponManager {

    @GetMapping("/coupon-management")
    public String couponManager() {
        return "adminpages/coupon-management/index";
    }
}
