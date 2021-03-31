package com.example.demo.controller;

import com.example.demo.model.Message;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class MessageController {

    @GetMapping("/message")
    Message send() {
            return new Message("Hello World from Argentina");
    }

    @PostMapping("/message")
    Message echo (@RequestBody Message message) {
        return message;
    }
}

