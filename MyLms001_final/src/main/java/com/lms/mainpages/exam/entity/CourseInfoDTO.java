package com.lms.mainpages.exam.entity;

public class CourseInfoDTO {
    private final String title;
    private final long instructorId;

    public CourseInfoDTO(String title, long instructorId) {
        this.title = title;
        this.instructorId = instructorId;
    }

    public String getTitle() {
        return title;
    }

    public long getInstructorId() {
        return instructorId;
    }
}