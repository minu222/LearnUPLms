package com.lms.mainpages.board.post.service;

import com.lms.mainpages.board.post.domain.Post;
import com.lms.mainpages.common.page.PageResponse;

public interface PostService {
    PageResponse<Post> list(String category, String q, int page, int size);
    Post get(Long id);
}
