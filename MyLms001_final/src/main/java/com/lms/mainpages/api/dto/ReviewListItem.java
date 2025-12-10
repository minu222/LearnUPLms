package com.lms.mainpages.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter@Setter
public class ReviewListItem {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private Long studentId;
    private String authorName;
}
