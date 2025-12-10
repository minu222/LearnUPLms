package com.lms.mainpages.board.post.repository;

public interface PostCommandRepository {
    Long insert(String title, String content, String category, Long userId);
}
