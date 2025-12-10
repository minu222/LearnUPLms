// src/main/java/dwacademy/mylms001/board/comment/dto/CommentDto.java
package com.lms.mainpages.board.comment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentDto {
    private Long id;               // comment_id
    private Long postId;           // post_id
    private Long userId;           // user_id
    private String content;
    private Long parentCommentId;  // parent_comment_id
    private Integer likes;         // nullable
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 표시용(조인 결과)
    private String authorName;     // users.name
    private String authorRole;     // users.role (admin/instructor/student)
}
