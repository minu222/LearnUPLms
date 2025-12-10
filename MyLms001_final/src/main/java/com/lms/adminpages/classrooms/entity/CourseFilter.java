package com.lms.adminpages.classrooms.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseFilter {
    private String category; // 카테고리 필터
    private String status;   // 상태 필터
    private String keyword;  // 검색어
}