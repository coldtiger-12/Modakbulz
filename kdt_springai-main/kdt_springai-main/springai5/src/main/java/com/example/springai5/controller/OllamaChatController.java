package com.example.springai5.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OllamaChatController {

    private final ChatClient chatClient;

    public OllamaChatController(@Qualifier("ollamaChatClient") ChatClient chatClient){
        this.chatClient=chatClient;
    }

    @GetMapping("/ollamaChat")
    public String chat() {
        return chatClient.prompt()
                .user("Ollama에 대한 소개글을 작성해줘")
                .call()
                .content();
    }
}