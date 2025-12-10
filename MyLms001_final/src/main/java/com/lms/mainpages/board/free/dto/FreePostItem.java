package com.lms.mainpages.board.free.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @ToString
@AllArgsConstructor @NoArgsConstructor @Builder
public class FreePostItem {
    private Long id;
    private String title;
    private LocalDateTime createdAt;

    // posts.user_id 기반으로 간단 생성 (DB에 사용자 테이블을 추가로 쓰지 않음)
    private Long authorId;
    private String authorName; // 예: "user#3"
    private String role;       // 예: "회원"
}
