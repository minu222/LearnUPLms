package com.lms.mainpages.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {

    private final JdbcTemplate jdbc;

    public CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 유저 장바구니에 해당 코스가 이미 있는지 */
    public boolean existsByUserIdAndCourseId(int userId, long courseId) {
        String sql = "SELECT COUNT(*) FROM cart WHERE user_id = ? AND course_id = ?";
        Integer cnt = jdbc.queryForObject(sql, Integer.class, userId, courseId);
        return cnt != null && cnt > 0;
    }

    /** 장바구니에서 해당 코스 제거 (결제 성공 시 호출) */
    public int deleteByUserIdAndCourseId(int userId, long courseId) {
        String sql = "DELETE FROM cart WHERE user_id = ? AND course_id = ?";
        return jdbc.update(sql, userId, courseId);
    }

    /** (선택) 장바구니에 담기 */
    public int insert(int userId, long courseId) {
        String sql = "INSERT INTO cart (user_id, course_id, added_at) VALUES (?, ?, NOW())";
        return jdbc.update(sql, userId, courseId);
    }
}
