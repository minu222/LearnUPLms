package com.lms.adminpages.email.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class Message {
    private Integer messageId;
    private Integer senderId;
    private Integer receiverId;
    private String content;
    private Boolean isRead;
    private Timestamp sentAt;

    // 추가: 상세 페이지에서 표시용
    private String senderName;
    private String receiverName;
}
