package com.lms.adminpages.users.service;

import com.lms.adminpages.users.dao.UserDao;
import com.lms.adminpages.users.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }


    public List<User> getInstructors(String status, String department, String keywordType, String keyword) {
        System.out.println(userDao.findInstructors(status, department, keywordType, keyword));
        return userDao.findInstructors(status, department, keywordType, keyword);
    }


    public List<User> getStudents(String status, String department, String keywordType, String keyword) {
        return userDao.findStudents(status, department, keywordType, keyword);
    }


    public void deleteUsers(int[] ids) {
        userDao.updateStatusToDeleted(ids);
    }


    public List<User> getMemberList(String group, String type, String keyword) {
        return userDao.findActiveMembers(group, type, keyword);
    }

    public List<User> getTrashList() {
        return userDao.findDeletedUsers();
    }

    public void deleteMembers(int[] userIds) {
        for (int id : userIds) {
            userDao.softDeleteById(id); // status=deleted 로 처리
        }
    }

    public void restoreUsers(List<Integer> ids) {
        userDao.restoreUsers(ids);
    }
}
