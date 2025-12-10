package com.lms.mainpages.web;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CourseForm {
    private String courseType;     // VOD | 개인강의 | 다수강의  -> category
    private String courseName;     // -> title
    private String description;    // -> description
    private String period;         // "YYYY-MM-DD ~ YYYY-MM-DD" -> expiry_date (끝)
    private String fee;            // number text -> price
    private String dailyTime;      // not stored
    private String introduction;   // not stored
    private String goal;           // not stored
}
