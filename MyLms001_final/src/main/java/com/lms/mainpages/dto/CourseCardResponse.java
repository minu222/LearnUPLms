package com.lms.mainpages.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

public record CourseCardResponse(
        long courseId,
        String title,
        String category,
        String status,          // published|draft|closed
        BigDecimal price,
        Boolean isFree,
        LocalDate expiryDate,
        Timestamp createdAt
) {

}

