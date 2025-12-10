package com.lms.mainpages.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter@Setter
public class PostSummaryDto {
    private Long id;
    private String title;
    private String content;
    private String category;
    private LocalDateTime createdAt;
}

