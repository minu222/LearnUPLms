// src/main/java/dwacademy/mylms001/service/CourseService.java
package com.lms.mainpages.service;

import com.lms.mainpages.repository.CourseMaterialRepository;
import com.lms.mainpages.repository.CourseRepository;
import com.lms.mainpages.web.CourseForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMaterialRepository materialRepository;

    /** period: "YYYY-MM-DD ~ YYYY-MM-DD" → 오른쪽(끝) 날짜 파싱 */
    private static final Pattern PERIOD = Pattern.compile(
            "\\s*(\\d{4}-\\d{2}-\\d{2})\\s*[~\\-–]\\s*(\\d{4}-\\d{2}-\\d{2})\\s*"
    );

    /** 강의 생성 + 첨부(메인/시험) 파일 디스크 저장 */
    @Transactional
    public long createCourse(int instructorId, CourseForm form,
                             MultipartFile mainImage, MultipartFile examFile) throws IOException {

        String category    = form.getCourseType();
        String title       = form.getCourseName();
        String description = form.getDescription();

        BigDecimal price = null;
        boolean isFree   = false;
        if (form.getFee() != null && !form.getFee().isBlank()) {
            try {
                price = new BigDecimal(form.getFee().trim());
                isFree = price.compareTo(BigDecimal.ZERO) == 0;
            } catch (NumberFormatException ignore) {}
        }

        LocalDate expiry = null;
        if (form.getPeriod() != null && !form.getPeriod().isBlank()) {
            Matcher m = PERIOD.matcher(form.getPeriod());
            if (m.find()) expiry = LocalDate.parse(m.group(2)); // 끝 날짜
        }

        long courseId = courseRepository.insertCourse(
                instructorId,
                title,
                description,
                category,
                price,
                isFree,
                "published", // 기본값
                0,           // 초기 수강생 수
                expiry
        );

        // 업로드 파일을 디스크에 보관(필요 시 경로만 저장하는 정책)
        saveBasicFiles(courseId, mainImage, examFile);

        return courseId;
    }

    /** 동영상 파일을 /uploads/course-materials 에 저장하고 course_materials에 1건 INSERT */
    @Transactional
    public void saveVideoMaterial(long courseId, MultipartFile videoFile) throws IOException {
        if (videoFile == null || videoFile.isEmpty()) return;

        Path base = Paths.get("uploads", "course-materials");
        Files.createDirectories(base);

        String orig = videoFile.getOriginalFilename();
        String sanitized = sanitize(orig);
        String filename  = "video_" + System.currentTimeMillis() + "_" + sanitized;

        Path dest = base.resolve(filename);
        Files.copy(videoFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        String absPath     = dest.toAbsolutePath().toString();
        String contentType = videoFile.getContentType();
        if (contentType == null) {
            try { contentType = Files.probeContentType(dest); } catch (IOException ignore) {}
        }
        if (contentType == null) contentType = "application/octet-stream";

        materialRepository.insert(
                courseId,
                (orig == null || orig.isBlank()) ? filename : orig, // name
                absPath,                                            // file_path
                contentType,                                        // file_type
                false,                                              // has_exam
                true                                                // has_replay(VOD)
        );
    }

    /* ================= 유틸 ================= */

    private void saveBasicFiles(long courseId, MultipartFile mainImage, MultipartFile examFile) throws IOException {
        Path base = Paths.get("uploads", "courses", String.valueOf(courseId));
        Files.createDirectories(base);

        if (mainImage != null && !mainImage.isEmpty()) {
            Path p = base.resolve("main_" + sanitize(mainImage.getOriginalFilename()));
            Files.copy(mainImage.getInputStream(), p, StandardCopyOption.REPLACE_EXISTING);
        }
        if (examFile != null && !examFile.isEmpty()) {
            Path p = base.resolve("exam_" + sanitize(examFile.getOriginalFilename()));
            Files.copy(examFile.getInputStream(), p, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** (선택) 강의 폴더 정리용 */
    private void rmCourseDirQuietly(long courseId) {
        Path base = Paths.get("uploads", "courses", String.valueOf(courseId));
        if (Files.notExists(base)) return;
        try (Stream<Path> walk = Files.walk(base)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignore) {}
            });
        } catch (IOException ignore) {}
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }


    @Transactional
    public void deleteCourseAdmin(long courseId) {
        if (!courseRepository.existsById(courseId)) throw new IllegalArgumentException("존재하지 않는 강의");
        int rows = courseRepository.deleteById(courseId);
        if (rows == 0) throw new IllegalStateException("삭제 실패");
        rmCourseDirQuietly(courseId);
    }

    @Transactional
    public void deleteCourseByTeacher(int instructorId, long courseId) {
        if (!courseRepository.existsByIdAndInstructor(courseId, instructorId))
            throw new IllegalArgumentException("권한이 없거나 강의가 없습니다.");
        int rows = courseRepository.deleteByIdAndInstructor(courseId, instructorId);
        if (rows == 0) throw new IllegalStateException("삭제 실패");
        rmCourseDirQuietly(courseId);
    }

}
