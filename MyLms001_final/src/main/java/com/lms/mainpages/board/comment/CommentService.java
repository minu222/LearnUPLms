// src/main/java/dwacademy/mylms001/board/comment/CommentService.java
package com.lms.mainpages.board.comment;

import com.lms.mainpages.board.comment.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository repo;

    public List<CommentDto> list(long postId) {
        return repo.findByPostId(postId);
    }

    @Transactional
    public long write(long postId, long userId, String content, Long parentCommentId) {
        if (!StringUtils.hasText(content)) return -1L;
        return repo.insert(postId, userId, content.trim(), parentCommentId);
    }

    @Transactional
    public boolean edit(long commentId, long userId, String content) {
        if (!StringUtils.hasText(content)) return false;
        return repo.updateContent(commentId, userId, content.trim()) > 0;
    }

    @Transactional
    public boolean delete(long commentId, long userId) {
        return repo.softDelete(commentId, userId) > 0;
    }
}
