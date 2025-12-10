package com.lms.adminpages.email.controller;

import com.lms.adminpages.email.entity.Message;
import com.lms.adminpages.email.service.MessageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/email")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/message-log")
    public String showMessage(Model model) {
        return "adminpages/message-log/index";
    }

    @GetMapping("/message-send")
    public String SendMessage(Model model) {
        return "adminpages/message-send/index";
    }


}