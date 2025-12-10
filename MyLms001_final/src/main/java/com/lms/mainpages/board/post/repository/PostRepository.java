package com.lms.mainpages.board.post.repository;

import com.lms.mainpages.board.post.domain.Post;

import java.util.List;

public interface PostRepository {
    List<Post> findPage(String category, String q, int page, int size);
    long count(String category, String q);
    Post findById(Long id);
}
