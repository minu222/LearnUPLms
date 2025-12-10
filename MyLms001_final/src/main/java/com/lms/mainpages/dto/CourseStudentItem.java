// src/main/java/dwacademy/mylms001/dto/CourseStudentItem.java
package com.lms.mainpages.dto;

public record CourseStudentItem(
        long userId,
        String photo,
        String name,
        String username,
        String gender,   // 남/여
        String email,
        Integer age,
        String intro
) {}

