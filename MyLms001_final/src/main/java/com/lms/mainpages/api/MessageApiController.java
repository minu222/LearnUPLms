package com.lms.mainpages.api;

import com.lms.mainpages.entity.Message;
import com.lms.mainpages.entity.User;
import com.lms.mainpages.service.MessageService;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageApiController {

    private final MessageService service;

    public MessageApiController(MessageService service) {
        this.service = service;
    }

    /** 받은/보낸함 */
    @GetMapping("/my")
    public ResponseEntity<?> my(@RequestParam(defaultValue = "inbox") String box,
                                HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        Long userId = Long.valueOf((long) login.getUser_id());
        List<Message> list = "sent".equalsIgnoreCase(box)
                ? service.sent(userId)
                : service.inbox(userId);
        return ResponseEntity.ok(list);
    }

    /** 전송 (receiverId 또는 receiverNickname 중 하나를 제공) */
    @PostMapping
    public ResponseEntity<?> send(@RequestBody SendReq req, HttpSession session) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }
        if (req == null || (isBlank(req.content) || (req.receiverId == null && isBlank(req.receiverNickname)))) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "받는사람과 내용을 입력하세요."));
        }

        Long senderId = Long.valueOf((long) login.getUser_id());
        Long receiverId = req.receiverId;

        if (receiverId == null) {
            receiverId = service.findUserIdByNickname(req.receiverNickname);
            if (receiverId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("ok", false, "message", "해당 닉네임의 사용자를 찾을 수 없습니다."));
            }
        }

        Long id = service.send(senderId, receiverId, req.content);
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }

    /** 읽음 설정 (이번 요구: 읽음으로 고정) */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 읽음만 허용 (false로 되돌리는 토글은 제공하지 않음)
        service.markRead(id, true);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    @Data
    public static class SendReq {
        public Long receiverId;        // 선택사항
        public String receiverNickname; // 선택사항
        public String content;         // 필수
    }
}
