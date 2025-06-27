package com.example.springai11.config;

import com.example.springai11.service.ChatService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {

    private final ChatService chatService;
    public CommandLineAppStartupRunner(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void run(String... args) throws Exception {
        chatService.startChat();
    }
}
