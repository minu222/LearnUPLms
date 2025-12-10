package com.lms.adminpages.classrooms.dao;

import com.lms.adminpages.classrooms.entity.CourseMaterial;
import org.apache.ibatis.annotations.Select;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CourseMaterialDAO {

    private final JdbcTemplate jdbcTemplate;

    public CourseMaterialDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<CourseMaterial> materialMapper = (rs, rowNum) -> CourseMaterial.builder()
            .materialId(rs.getInt("material_id"))
            .courseId(rs.getInt("course_id"))
            .courseTitle(rs.getString("course_title"))
            .name(rs.getString("name"))
            .filePath(rs.getString("file_path"))
            .fileType(rs.getString("file_type"))
            .hasExam(rs.getBoolean("has_exam"))
            .hasReplay(rs.getBoolean("has_replay"))
            .build();


    //수업자료 목록에서 전체 자료 조회
    public List<CourseMaterial> findAll() {
        String sql = """
        SELECT m.material_id, m.course_id, m.name, m.file_path, m.file_type, m.has_exam, m.has_replay,
               c.title AS course_title
        FROM course_materials m
        JOIN courses c ON m.course_id = c.course_id
    """;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(CourseMaterial.class));
    }


    //수업자료 목록에서 강의실명으로 검색기능
    public List<CourseMaterial> searchByCourseTitle(String keyword) {
        String sql = """
        SELECT m.material_id, m.course_id, m.name, m.file_path, m.file_type, m.has_exam, m.has_replay,
               c.title AS course_title
        FROM course_materials m
        JOIN courses c ON m.course_id = c.course_id
        WHERE c.title LIKE ?
    """;

        return jdbcTemplate.query(
                sql,
                new BeanPropertyRowMapper<>(CourseMaterial.class),
                "%" + keyword + "%"
        );
    }
    // 단건 조회
    public CourseMaterial findById(Integer id) {
        String sql = """
        SELECT m.material_id,
               m.course_id,
               m.name,
               m.file_path,
               m.file_type,
               m.has_exam,
               m.has_replay,
               c.title AS course_title
        FROM course_materials m
        LEFT JOIN courses c ON m.course_id = c.course_id
        WHERE m.material_id = ?
    """;

        return jdbcTemplate.queryForObject(sql, materialMapper, id);
    }
    // 저장
    public int save(CourseMaterial material) {
        String sql = "INSERT INTO course_materials (course_id, name, file_path, file_type, has_exam, has_replay) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                material.getCourseId(),
                material.getName(),
                material.getFilePath(),
                material.getFileType(),
                material.getHasExam(),
                material.getHasReplay()
        );
    }

    // 수정
    public int update(CourseMaterial material) {
        String sql = "UPDATE course_materials SET course_id=?, name=?, file_path=?, file_type=?, has_exam=?, has_replay=? " +
                "WHERE material_id=?";
        return jdbcTemplate.update(sql,
                material.getCourseId(),
                material.getName(),
                material.getFilePath(),
                material.getFileType(),
                material.getHasExam(),
                material.getHasReplay(),
                material.getMaterialId()
        );
    }

    // 삭제
    public int delete(Integer id) {
        String sql = "DELETE FROM course_materials WHERE material_id=?";
        return jdbcTemplate.update(sql, id);
    }


}