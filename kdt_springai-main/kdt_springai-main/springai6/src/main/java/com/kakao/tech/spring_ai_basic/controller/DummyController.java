package com.kakao.tech.spring_ai_basic.controller;

import com.kakao.tech.spring_ai_basic.dto.User;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("dummy")
public class DummyController {

    private final ChatClient chatClient;

    public DummyController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping
    public List<User> dummy() {
        return chatClient.prompt()
                .user("더미 데이터를 10개 생성해줘")
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }
}
