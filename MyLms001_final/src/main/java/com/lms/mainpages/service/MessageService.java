package com.lms.mainpages.service;

import com.lms.mainpages.entity.Message;
import com.lms.mainpages.repository.MessageDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("mainMessageService")
@RequiredArgsConstructor
public class MessageService {

    private final MessageDao dao;

    public List<Message> inbox(Long userId) {
        return dao.findInbox(userId);
    }

    public List<Message> sent(Long userId) {
        return dao.findSent(userId);
    }

    public Long send(Long senderId, Long receiverId, String content) {
        return dao.save(senderId, receiverId, content);
    }

    public void markRead(Long id, boolean read) {
        dao.markRead(id, read);
    }

    public void delete(Long id) {
        dao.delete(id);
    }

    public Long findUserIdByNickname(String nickname) {
        return dao.findUserIdByNickname(nickname);
    }
}
