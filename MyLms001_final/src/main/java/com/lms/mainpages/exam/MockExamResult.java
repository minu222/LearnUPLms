package com.lms.mainpages.exam;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class MockExamResult {
    private Integer resultId;
    private Integer examId;
    private String courseTitle;
    private Integer studentId;
    private String answers;
    private Timestamp takenAt;
    private Integer totalScore;
}
