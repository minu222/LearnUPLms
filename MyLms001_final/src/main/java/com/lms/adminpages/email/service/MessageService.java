package com.lms.adminpages.email.service;

import com.lms.adminpages.email.dao.MessageDao;
import com.lms.adminpages.email.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("adminMessageService")
public class MessageService {

    private final MessageDao messageDao;


    public MessageService(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    public void sendMessage(Message message) {
        messageDao.sendMessage(message);
    }

    public List<Message> getReceivedMessages(int receiverId) {
        return messageDao.getReceivedMessages(receiverId);
    }

    public List<Message> getSentMessages(int senderId) {
        return messageDao.getSentMessages(senderId);
    }

    public Message getMessageDetail(int messageId) {
        messageDao.markAsRead(messageId); // 상세조회 시 자동 읽음 처리
        return messageDao.getMessageDetail(messageId);
    }

    public void deleteMessage(int messageId) {
        messageDao.deleteMessage(messageId);
    }
}