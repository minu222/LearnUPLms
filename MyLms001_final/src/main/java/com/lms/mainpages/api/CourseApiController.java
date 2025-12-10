// src/main/java/dwacademy/mylms001/controller/CourseApiController.java
package com.lms.mainpages.api;

import com.lms.mainpages.exceptoin.NotFoundException;
import com.lms.mainpages.service.CourseService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseApiController {

    private final CourseService courseService;

    public CourseApiController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** (관리자) 강의 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        courseService.deleteCourseAdmin(id);   // ✅ 이제 존재
        return ResponseEntity.noContent().build(); // 204
    }

    /** 존재하지 않을 때 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404
    }

    /** FK 제약 등으로 삭제 불가할 때 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("참조 데이터가 있어 삭제할 수 없습니다."); // 409
    }
}
