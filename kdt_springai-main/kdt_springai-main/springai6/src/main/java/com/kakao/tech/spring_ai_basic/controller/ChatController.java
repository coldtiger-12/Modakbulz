package com.kakao.tech.spring_ai_basic.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping()
    public String chat(@RequestParam(name = "q") String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    @GetMapping("/hello")
    public String chatWithTemplate(@RequestParam(name = "name") String name) {
        PromptTemplate promptTemplate = new PromptTemplate("안녕? 내 이름은 {name} 이야");
        Prompt prompt = promptTemplate.create(Map.of("name", name));
        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}
