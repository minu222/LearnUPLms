package com.lms.mainpages.api;

import com.lms.mainpages.api.dto.PostSummaryDto;
import com.lms.mainpages.service.MyPostsJdbcService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/my/posts")
public class MyPostsApiController {

    private final MyPostsJdbcService svc;

    public MyPostsApiController(MyPostsJdbcService svc) {
        this.svc = svc;
    }

    // ===== 로그인 객체 탐색 =====
    /** 대표적인 세션 키를 우선 조회하고, 없으면 모든 세션 속성 중 User/Member/Account 류를 스캔 */
    private Object resolveLoginPrincipal(HttpSession session) {
        if (session == null) return null;

        // 흔한 키 우선
        String[] keys = {"loginUser","user","member","principal","currentUser","authUser"};
        for (String k : keys) {
            Object v = session.getAttribute(k);
            if (v != null) return v;
        }

        // 그 외: 세션의 모든 속성 중 클래스명이 User/Member/Account/Principal 를 포함하는 객체 탐색
        try {
            Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Object v = session.getAttribute(name);
                if (v == null) continue;
                String cn = v.getClass().getSimpleName().toLowerCase(Locale.ROOT);
                if (cn.contains("user") || cn.contains("member") || cn.contains("account") || cn.contains("principal")) {
                    return v;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    // ===== 사용자 ID 추출 (매우 관대하게) =====
    /** 숫자/숫자문자열을 Long으로 파싱 */
    private Long toLongOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number)o).longValue();
        try {
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception ignored) {}
        return null;
    }

    /** 주어진 객체에서 게터를 통해 ID 후보 추출 */
    private Long extractIdByCommonGetters(Object obj) {
        String[] getters = {
                "getUser_id","getUserId","getId",
                "getUser_no","getUserNo","getMemberId","getMember_id",
                "getMemberNo","getMember_no","getSeq","getUserSeq","getUser_seq","getNo"
        };
        for (String g : getters) {
            try {
                Method m = obj.getClass().getMethod(g);
                Object v = m.invoke(obj);
                Long id = toLongOrNull(v);
                if (id != null) return id;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** 모든 public 게터를 훑어서 (user.*id | .*Id | .*No | .*Seq) 같은 패턴의 숫자 반환을 찾음 */
    private Long extractIdByHeuristics(Object obj) {
        try {
            for (Method m : obj.getClass().getMethods()) {
                String n = m.getName();
                if (!n.startsWith("get") || m.getParameterCount() != 0) continue;
                String ln = n.toLowerCase(Locale.ROOT);
                boolean looksLikeId =
                        ln.contains("userid") || ln.contains("user_id") ||
                                (ln.endsWith("id")) || ln.endsWith("no") || ln.endsWith("seq");
                if (!looksLikeId) continue;
                Object v = m.invoke(obj);
                Long id = toLongOrNull(v);
                if (id != null) return id;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** getUser()/getMember()/getAccount() 처럼 한 단계 중첩 객체에서 다시 추출 */
    private Long extractIdFromNested(Object obj) {
        String[] nesters = {"getUser","getMember","getAccount","getPrincipal","getProfile"};
        for (String nest : nesters) {
            try {
                Method m = obj.getClass().getMethod(nest);
                Object nested = m.invoke(obj);
                if (nested == null) continue;
                Long id = extractIdByCommonGetters(nested);
                if (id != null) return id;
                id = extractIdByHeuristics(nested);
                if (id != null) return id;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** 최종: 세션에서 로그인 객체 찾고, 다양한 규칙으로 Long ID 추출 */
    private Long resolveUserId(HttpSession session) {
        Object principal = resolveLoginPrincipal(session);
        if (principal == null) return null;

        // 1순위: 자주 쓰는 게터들
        Long id = extractIdByCommonGetters(principal);
        if (id != null) return id;

        // 2순위: 휴리스틱(게터 이름 패턴)
        id = extractIdByHeuristics(principal);
        if (id != null) return id;

        // 3순위: 중첩 객체 안에서 재시도
        return extractIdFromNested(principal);
    }

    // ===== API =====
    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Long uid = resolveUserId(session);
        // 디버그 로그
        System.out.println("[MyPostsApi] resolved uid=" + uid);

        if (uid == null) {
            // 인증 실패로 처리 (프런트가 로그인 페이지로 보냄)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false, "message", "login required"));
        }

        var list = svc.list(uid);
        var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        var body = list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("content", p.getContent());
            m.put("category", p.getCategory());
            m.put("createdAt", p.getCreatedAt() == null ? "" : dtf.format(p.getCreatedAt()));
            String cat = Optional.ofNullable(p.getCategory()).orElse("").toLowerCase(Locale.ROOT);
            m.put("typeLabel", "qna".equals(cat) ? "문의게시판" : "자유게시판");
            return m;
        }).collect(Collectors.toList());

        System.out.println("[MyPostsApi] resultCount=" + body.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpSession session) {
        Long uid = resolveUserId(session);
        if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        PostSummaryDto p = svc.one(id, uid);
        if (p == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ok", false, "message", "게시글이 없습니다."));
        return ResponseEntity.ok(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, String> req,
                                    HttpSession session) {
        Long uid = resolveUserId(session);
        if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));

        String title   = Optional.ofNullable(req.get("title")).orElse("").trim();
        String content = Optional.ofNullable(req.get("content")).orElse("").trim();
        if (title.isEmpty() && content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "수정할 내용이 없습니다."));
        }
        int rows = svc.update(id, uid, title, content);
        if (rows == 0) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("ok", false));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Long uid = resolveUserId(session);
        if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        int rows = svc.delete(id, uid);
        if (rows == 0) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("ok", false));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // 401 매핑용 (지금은 사용 안 함)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) static class Unauthorized extends RuntimeException {}
}
