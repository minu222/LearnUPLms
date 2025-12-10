package com.lms.adminpages.classrooms.entity;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockExam {
    private Integer examId;
    private Integer instructorId;
    private String courseTitle;
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String answer;
    private Integer score;
    private Timestamp takenAt;

    // JOIN해서 표시할 때 쓸 필드
    private String instructorName;
    private String studentName;

    @Override
    public String toString() {
        return "MockExam{" +
                "examId=" + examId +
                ", instructorId=" + instructorId +
                ", courseTitle='" + courseTitle + '\'' +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", score=" + score +
                ", takenAt=" + takenAt +
                ", instructorName='" + instructorName + '\'' +
                ", studentName='" + studentName + '\'' +
                '}';
    }

}
