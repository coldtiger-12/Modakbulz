package com.kakao.tech.spring_ai_basic.controller;

import com.kakao.tech.spring_ai_basic.service.HackNewsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("news")
public class HackerNewsController {

    private final ChatClient chatClient;

    public HackerNewsController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping
    public String showNewsPage() {
        return "news";
    }

    @PostMapping
    @ResponseBody
    public String getNews(@RequestParam("request") String request) {
        return chatClient.prompt()
                .system("당신은 뉴스 전문가입니다. 사용자의 질문에서 주제를 영문으로 추출하여 최신 뉴스를 제공합니다. 최종 결과물은 한글로 작성합니다.")
                .tools(new HackNewsService())
                .user(request)
                .call()
                .content();
    }
}
