package com.lms.adminpages.classrooms.service;

import com.lms.adminpages.classrooms.dao.MockExamDAO;
import com.lms.adminpages.classrooms.entity.MockExam;
import com.lms.adminpages.users.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockExamService {

    @Autowired
    private MockExamDAO mockExamDao;



    public List<MockExam> getAllExams() {
        return mockExamDao.findAll();
    }

    /// 강사 목록 조회
    public List<User> findAllInstructors() {
        return mockExamDao.findAllInstructors();
    }

    // instructorId로 시험 문제 검색
    public List<MockExam> findByInstructorName(String instructorName) {
        return mockExamDao.findByInstructorName(instructorName);
    }

    public MockExam getExamById(int examId) {
        MockExam exam = mockExamDao.findById(examId);
        if (exam == null) {
            throw new RuntimeException("시험 문제를 찾을 수 없습니다. ID=" + examId);
        }
        return exam;
    }

    public void saveExam(MockExam exam) {
        mockExamDao.save(exam);
    }

    public void updateExam(MockExam exam) {
        mockExamDao.update(exam);
    }

    public void deleteExam(int examId) {
        mockExamDao.delete(examId);
    }


}