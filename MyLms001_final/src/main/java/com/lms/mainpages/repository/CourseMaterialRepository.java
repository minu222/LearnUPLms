// src/main/java/dwacademy/mylms001/repository/CourseMaterialRepository.java
package com.lms.mainpages.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CourseMaterialRepository {

    /** 스키마가 있으면 "project.course_materials" 로 바꾸세요. */
    private static final String TABLE = "course_materials";

    private final JdbcTemplate jdbc;

    public CourseMaterialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ===== DTO =====
    public record Material(
            long materialId,
            long courseId,
            String name,
            String filePath,
            String fileType,
            boolean hasExam,
            boolean hasReplay
    ) {}

    private static final RowMapper<Material> MAPPER = (rs, n) -> new Material(
            rs.getLong("material_id"),
            rs.getLong("course_id"),
            rs.getString("name"),
            rs.getString("file_path"),
            rs.getString("file_type"),
            rs.getInt("has_exam") == 1,
            rs.getInt("has_replay") == 1
    );

    /** 첫 번째 재생 가능한 동영상(파일타입 video/* 또는 확장자 mp4/webm/mov/m4v, 혹은 has_replay=1) */
    public Optional<Material> findFirstPlayableVideo(long courseId) {
        String sql =
                "SELECT material_id, course_id, name, file_path, file_type, has_exam, has_replay " +
                        "FROM " + TABLE + " " +
                        "WHERE course_id = ? AND (" +
                        "      file_type LIKE 'video/%' " +
                        "   OR LOWER(file_path) LIKE '%.mp4' " +
                        "   OR LOWER(file_path) LIKE '%.webm' " +
                        "   OR LOWER(file_path) LIKE '%.mov' " +
                        "   OR LOWER(file_path) LIKE '%.m4v' " +
                        "   OR has_replay = 1" +
                        ") ORDER BY material_id ASC LIMIT 1";

        return jdbc.query(sql, ps -> ps.setLong(1, courseId), rs -> {
            if (rs.next()) return Optional.of(MAPPER.mapRow(rs, 1));
            return Optional.empty();
        });
    }

    /** (호환용) 기존 메서드명 유지가 필요하면 이걸 컨트롤러에서 호출해도 됩니다. */
    public Optional<Material> findFirstVideoByCourseId(long courseId) {
        return findFirstPlayableVideo(courseId);
    }

    /** 해당 코스의 모든 동영상 자료 목록 */
    public List<Material> findAllVideosByCourseId(long courseId) {
        String sql =
                "SELECT material_id, course_id, name, file_path, file_type, has_exam, has_replay " +
                        "FROM " + TABLE + " " +
                        "WHERE course_id = ? AND (" +
                        "      file_type LIKE 'video/%' " +
                        "   OR LOWER(file_path) LIKE '%.mp4' " +
                        "   OR LOWER(file_path) LIKE '%.webm' " +
                        "   OR LOWER(file_path) LIKE '%.mov' " +
                        "   OR LOWER(file_path) LIKE '%.m4v' " +
                        "   OR has_replay = 1" +
                        ") ORDER BY material_id ASC";

        return jdbc.query(sql, MAPPER, courseId);
    }

    /** materialId 단건 조회 */
    public Optional<Material> findById(long materialId) {
        String sql =
                "SELECT material_id, course_id, name, file_path, file_type, has_exam, has_replay " +
                        "FROM " + TABLE + " WHERE material_id = ?";
        return jdbc.query(sql, ps -> ps.setLong(1, materialId), rs -> {
            if (rs.next()) return Optional.of(MAPPER.mapRow(rs, 1));
            return Optional.empty();
        });
    }

    /** 자료 INSERT (업로드 저장 후 DB 기록) */
    public long insert(long courseId,
                       String name,
                       String filePath,
                       String fileType,
                       boolean hasExam,
                       boolean hasReplay) {
        String sql =
                "INSERT INTO " + TABLE +
                        " (course_id, name, file_path, file_type, has_exam, has_replay) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, courseId);
            ps.setString(i++, name);
            ps.setString(i++, filePath);
            ps.setString(i++, fileType);
            ps.setInt(i++, hasExam ? 1 : 0);
            ps.setInt(i++, hasReplay ? 1 : 0);
            return ps;
        }, kh);

        Number key = kh.getKey();
        return key == null ? 0L : key.longValue();
    }

    /** 파일 존재 확인 유틸(선택) */
    public boolean fileExists(String absPath) {
        try { return Files.exists(Path.of(absPath)); } catch (Exception e) { return false; }
    }
}
