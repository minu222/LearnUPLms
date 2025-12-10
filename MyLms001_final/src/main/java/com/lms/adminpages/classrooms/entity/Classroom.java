package com.lms.adminpages.classrooms.entity;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classroom {

    private Integer classroomId;   // PRIMARY KEY, AUTO_INCREMENT

    private Integer instructorId;  // 외래키 (optional)

    private String title;          // 강의실명/강좌명

    private String description;    // 설명

    private String category;       // 카테고리

    private BigDecimal price;      // 가격

    private Boolean isFree;        // 무료 여부

    private BigDecimal avgRating;  // 평균 평점

    private String status;         // 상태: draft/published/closed

    private Integer studentCount;  // 학생 수

    private LocalDate expiryDate;  // 만료일

    private Integer liveLimit;     // 라이브 수업 제한

    private LocalDateTime createdAt; // 생성일

    private LocalDateTime updatedAt; // 수정일

    private LocalDateTime deletedAt; // 삭제일 (soft delete)

    private String instructorNickname;



    // 수업자료 업로드용
    private List<MultipartFile> materialFiles = new ArrayList<>();
    private List<String> materialNames = new ArrayList<>();

    // 시험 포함 여부 체크
    private List<Boolean> hasExam = new ArrayList<>();

    // 시험자료
    private List<MockExam> exams = new ArrayList<>();
}