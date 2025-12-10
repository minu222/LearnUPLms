package com.lms.mainpages.board.post.controller;

import com.lms.mainpages.board.post.service.PostService;
import com.lms.mainpages.common.page.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    // GET /api/posts?category=NOTICE|free&page=0&size=10&q=검색어
    @GetMapping
    public PageResponse<PostListItem> list(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        var p = postService.list(category, q, page, size);
        return PageResponse.<PostListItem>builder()
                .content(p.getContent().stream()
                        .map(v -> new PostListItem(v.getId(), v.getTitle(), v.getCategory(), v.getCreatedAt()))
                        .toList())
                .number(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    // GET /api/posts/{id}
    @GetMapping("/{id}")
    public PostDetail get(@PathVariable Long id) {
        var v = postService.get(id);
        return new PostDetail(v.getId(), v.getTitle(), v.getContent(), v.getCategory(), v.getCreatedAt());
    }

    public record PostListItem(Long id, String title, String category, java.time.LocalDateTime createdAt) {}
    public record PostDetail(Long id, String title, String content, String category, java.time.LocalDateTime createdAt) {}
}
