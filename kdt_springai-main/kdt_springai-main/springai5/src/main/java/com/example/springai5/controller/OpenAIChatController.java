package com.example.springai5.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenAIChatController {

    private final ChatClient chatClient;

    public OpenAIChatController(@Qualifier("openAIChatClient") ChatClient chatClient){
        this.chatClient=chatClient;
    }

    @GetMapping("/openAIChat")
    public String chat() {
        return chatClient.prompt()
                .user("OpenAI에 대한 소개글을 작성해줘")
                .call()
                .content();
    }
}