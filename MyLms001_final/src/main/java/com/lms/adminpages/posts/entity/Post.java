package com.lms.adminpages.posts.entity;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    private Integer postId;        // post_id
    private Integer userId;        // 작성자 id
    private String title;
    private String content;
    private String category;       // FREE, NOTICE
    private Integer views;         // 조회수
    private Integer likes;         // 좋아요수
    private Integer commentsCount; // 댓글수
    private Boolean isDeleted;     // 삭제 여부
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 화면용 작성자 이름
    private String authorName;
}
