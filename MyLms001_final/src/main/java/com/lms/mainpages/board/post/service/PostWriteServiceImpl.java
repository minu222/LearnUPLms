package com.lms.mainpages.board.post.service;

import com.lms.mainpages.board.post.repository.PostCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostWriteServiceImpl implements PostWriteService {

    private final PostCommandRepository commandRepo;

    @Override
    @Transactional
    public Long write(String title, String content, String category, Long userId) {
        return commandRepo.insert(title, content, category, userId);
    }
}

