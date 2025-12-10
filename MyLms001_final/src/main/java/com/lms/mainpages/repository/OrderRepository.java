package com.lms.mainpages.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 결제 완료 주문 INSERT 후 생성된 order_id 반환 */
    public long insertPaidOrder(long userId, long totalAmount, String paymentMethod) {
        String sql = """
            INSERT INTO orders (user_id, total_amount, status, payment_method, created_at)
            VALUES (?, ?, 'paid', ?, NOW())
        """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setLong(2, totalAmount);
            ps.setString(3, paymentMethod != null ? paymentMethod : "card");
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : 0L;
    }

    /** 이미 수강중인지(enrollments.student_id / course_id) */
    public boolean existsEnrollment(long userId, long courseId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_id = ?";
        Integer cnt = jdbc.queryForObject(sql, Integer.class, userId, courseId);
        return cnt != null && cnt > 0;
    }

    /** 이미 결제 완료된 동일 강의가 있는지(orders.user_id + enrollments.course_id, status=paid) */
    public boolean existsPaidByUserIdAndCourseId(long userId, long courseId) {
        String sql = """
            SELECT COUNT(*)
              FROM orders o
              JOIN enrollments e ON e.order_id = o.order_id
             WHERE o.user_id = ? AND e.course_id = ? AND LOWER(o.status) = 'paid'
        """;
        Integer cnt = jdbc.queryForObject(sql, Integer.class, userId, courseId);
        return cnt != null && cnt > 0;
    }

    /** 수강 등록: enrollments (order_id, student_id, course_id, enrolled_at) */
    public void insertEnrollment(long orderId, long userId, long courseId) {
        String sql = """
            INSERT INTO enrollments (order_id, student_id, course_id, enrolled_at)
            VALUES (?, ?, ?, NOW())
        """;
        jdbc.update(sql, orderId, userId, courseId);
    }

    /** 강의 가격/무료 여부 조회 (courses.price, courses.is_free 가정) */
    public Map<String, Object> findCoursePriceRow(long courseId) {
        String sql = "SELECT is_free, price FROM courses WHERE course_id = ?";
        return jdbc.queryForMap(sql, courseId);
    }

    /** 장바구니에서 삭제 (cart.user_id, cart.course_id) */
    public int deleteCartItem(long userId, long courseId) {
        String sql = "DELETE FROM cart WHERE user_id = ? AND course_id = ?";
        return jdbc.update(sql, userId, courseId);
    }

    /** 내 주문 요약: item_count + 단일이면 first_title(강의명) 포함 */
    public List<Map<String, Object>> listByUser(long userId) {
        String sql = """
            SELECT o.order_id,
                   o.total_amount,
                   o.status,
                   o.payment_method,
                   o.created_at,
                   COUNT(e.enrollment_id) AS item_count,
                   CASE WHEN COUNT(e.enrollment_id) = 1 THEN MIN(c.title) ELSE NULL END AS first_title
              FROM orders o
              LEFT JOIN enrollments e ON e.order_id = o.order_id
              LEFT JOIN courses     c ON c.course_id = e.course_id
             WHERE o.user_id = ?
             GROUP BY o.order_id
             ORDER BY o.created_at DESC
        """;
        return jdbc.queryForList(sql, userId);
    }
}
