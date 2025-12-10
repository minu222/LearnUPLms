package com.lms.mainpages.board.post.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    private Long id;
    private String title;
    private String content;
    private String category;       // "NOTICE" | "free"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
