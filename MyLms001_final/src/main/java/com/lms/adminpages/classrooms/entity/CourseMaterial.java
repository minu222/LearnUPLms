package com.lms.adminpages.classrooms.entity;

import lombok.*;


@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMaterial {
    private Integer materialId;   //자료id
    private Integer courseId;     //강의실id
    private String courseTitle;   //강의실명
    private String name;          //파일명
    private String filePath;      //파일경로
    private String fileType;      //파일타입
    private Boolean hasExam;      //시험자료 포함
    private Boolean hasReplay;    //다시보기 여부

}