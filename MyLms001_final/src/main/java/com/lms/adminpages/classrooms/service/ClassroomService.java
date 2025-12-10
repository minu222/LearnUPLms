package com.lms.adminpages.classrooms.service;

import com.lms.adminpages.classrooms.dao.ClassroomDAO;
import com.lms.adminpages.classrooms.entity.Classroom;
import com.lms.adminpages.classrooms.entity.CourseFilter;
import com.lms.adminpages.classrooms.entity.StudentDto;
import com.lms.adminpages.users.dao.UserDao;
import com.lms.adminpages.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service
public class ClassroomService {

    private final ClassroomDAO classroomDao;
    private final UserDao userDao;


    public ClassroomService(ClassroomDAO classroomDao, UserDao userDao) {
        this.classroomDao = classroomDao;
        this.userDao = userDao;
    }

    @Transactional
    public void save(Classroom classroom) {
        classroomDao.save(classroom);
    }

    public List<User> findAllInstructors()
    {
        return classroomDao.findAllInstructors();
    }

    public List<String> findAllCategories() {
        return classroomDao.findAllCategories();
    }

    public List<Classroom> findByFilterFromDB(CourseFilter filter) {
        return classroomDao.findByFilterFromDB(filter);
    }

    public List<Classroom> findAll() {
        return classroomDao.findAll();
    }

    public User findUserById(Integer userId) {
        return userDao.findById(userId);
    }

    public List<Classroom> searchClassrooms(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return classroomDao.findAll(); // 전체 조회
        }
        return classroomDao.findClassrooms(keyword); // 키워드 검색
    }

    public List<StudentDto> getStudentsByCourseId(int courseId) {
        return classroomDao.findStudentsByCourseId(courseId);
    }


    //상태 수정
    @Transactional
    public void updateStatus(Map<Integer, String> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) return;
        statusMap.forEach(classroomDao::updateStatus);
    }

    // 선택 삭제
    @Transactional
    public void deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        classroomDao.deleteByIds(ids);
    }




}
