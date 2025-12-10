package com.lms.mainpages.board.post.service;

import com.lms.mainpages.board.post.domain.Post;
import com.lms.mainpages.board.post.repository.PostRepository;
import com.lms.mainpages.common.page.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository repo;

    @Override
    public PageResponse<Post> list(String category, String q, int page, int size) {
        long total = repo.count(category, q);
        int totalPages = (int) Math.ceil(total / (double) size);
        List<Post> content = repo.findPage(category, q, page, size);

        return PageResponse.<Post>builder()
                .content(content)
                .number(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .first(page <= 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .build();
    }

    @Override
    public Post get(Long id) {
        Post p = repo.findById(id);
        if (p == null) throw new IllegalArgumentException("게시글 없음: " + id);
        return p;
    }
}
