package com.lms.mainpages.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter@Setter
public class ReviewDetailDto {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Long studentId;
    private String authorName;
}
