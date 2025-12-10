package com.lms.mainpages.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;                 // messages.message_id
    private Long senderId;           // sender_id
    private Long receiverId;         // receiver_id

    // JOIN users.* (보낸사람/받는사람 프로필 표시용)
    private String senderNickname;
    private String receiverNickname;
    private String senderName;
    private String receiverName;
    private String senderRole;       // admin | instructor | student
    private String receiverRole;     // admin | instructor | student

    private String content;          // 내용
    private boolean read;            // is_read
    private LocalDateTime sentAt;    // 보낸 시각
}
