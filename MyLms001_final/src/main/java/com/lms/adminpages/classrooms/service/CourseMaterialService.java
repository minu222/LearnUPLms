package com.lms.adminpages.classrooms.service;

import com.lms.adminpages.classrooms.dao.CourseMaterialDAO;
import com.lms.adminpages.classrooms.entity.CourseMaterial;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseMaterialService {

    private final CourseMaterialDAO courseMaterialDao;

    public CourseMaterialService(CourseMaterialDAO courseMaterialDao) {
        this.courseMaterialDao = courseMaterialDao;
    }

    public List<CourseMaterial> searchMaterials(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return courseMaterialDao.findAll(); // 검색어 없으면 전체 조회
        }
        return courseMaterialDao.searchByCourseTitle(keyword); // 검색어 있으면 검색
    }

    public CourseMaterial getMaterialById(int materialId) {
        return courseMaterialDao.findById(materialId);
    }

    public CourseMaterial getMaterial(Integer id) {
        return courseMaterialDao.findById(id);
    }

    public void saveMaterial(CourseMaterial material) {
        courseMaterialDao.save(material);
    }

    public void updateMaterial(CourseMaterial material) {
        courseMaterialDao.update(material);
    }

    public void deleteMaterial(Integer id) {
        courseMaterialDao.delete(id);
    }
}