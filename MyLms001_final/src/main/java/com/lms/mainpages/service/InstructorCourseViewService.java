// src/main/java/dwacademy/mylms001/service/InstructorCourseViewService.java
package com.lms.mainpages.service;

import com.lms.mainpages.dto.CourseCardResponse;
import com.lms.mainpages.dto.CourseStudentItem;
import com.lms.mainpages.repository.InstructorCourseViewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstructorCourseViewService {

    private final InstructorCourseViewRepository repo;

    public InstructorCourseViewService(InstructorCourseViewRepository repo) {
        this.repo = repo;
    }

    private static String toDbStatus(String kor) {
        if (kor == null || kor.isBlank() || "전체".equals(kor)) return null;
        return switch (kor) {
            case "정상" -> "published";
            case "중지" -> "closed";
            case "임시" -> "draft";
            default -> null;
        };
    }

    public List<CourseCardResponse> listForInstructor(int instructorId, String statusKor, String q) {
        return repo.findCardsByInstructor(instructorId, toDbStatus(statusKor), q);
    }

    public List<CourseStudentItem> students(long courseId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        return repo.findStudents(courseId, offset, size);
    }

    public int studentCount(long courseId) {
        return repo.countStudents(courseId);
    }
}

