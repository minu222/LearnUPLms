package com.lms.adminpages.dashboard.service;

import com.lms.adminpages.dashboard.dao.NotificationDao;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationDao notificationDao;

    public NotificationService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public List<String> getRecentNotifications() {
        return notificationDao.getRecentNotifications();
    }
}