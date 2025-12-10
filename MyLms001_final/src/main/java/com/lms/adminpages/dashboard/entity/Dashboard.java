package com.lms.adminpages.dashboard.entity;

import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {
    private int studentCount;     // 총 학생 수
    private int teacherCount;     // 총 강사 수
    private int courseCount;      // 개설 과정 수
    private double avgCompletion; // 평균 수료율
    private double cancelRate;    // 수강 취소율
    private double monthRevenue;  // 이번 달 매출
}