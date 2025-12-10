package com.lms.mainpages.board.post.service;

public interface PostWriteService {
    Long write(String title, String content, String category, Long userId);
}
