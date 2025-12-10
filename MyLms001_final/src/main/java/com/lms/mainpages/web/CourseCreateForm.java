// src/main/java/dwacademy/mylms001/web/CourseCreateForm.java
package com.lms.mainpages.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CourseCreateForm {

    @NotBlank(message = "과정명을 입력하세요.")
    private String courseName;

    @NotNull(message = "교육비를 입력하세요.")
    @Min(value = 0, message = "교육비는 0 이상이어야 합니다.")
    private Integer fee;

    @NotBlank(message = "교육 기간을 입력하세요.")
    private String period;       // "2025-01-01 ~ 2025-02-01" 등

    @NotBlank(message = "과정설명을 입력하세요.")
    private String description;

    // 나머지 필드가 있으면 추가 (courseType, intro, goal 등)
}

