package com.lms.adminpages.classrooms.entity;

import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDto {
    private int userId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private LocalDate birthDay;
    private String gender;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime enrolledAt;
}