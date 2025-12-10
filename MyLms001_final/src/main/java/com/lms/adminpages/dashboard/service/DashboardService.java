package com.lms.adminpages.dashboard.service;

import com.lms.adminpages.dashboard.dao.DashboardDao;
import com.lms.adminpages.dashboard.entity.Dashboard;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardDao dashboardDao;

    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    public Dashboard getDashboardData() {
        return dashboardDao.getDashboardData();
    }
}