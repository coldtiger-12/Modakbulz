package com.example.springai14.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class HotelController {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public HotelController(VectorStore vectorStore, ChatClient.Builder chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient.build();
    }

    @GetMapping("/question")
    public Flux<String> recommendMovies1(@RequestParam("question") String question, Model model) throws Exception {
        System.out.println(question);
        // Fetch similar movies using vector store
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query(question).similarityThreshold(0.5).topK(1).build());
        System.out.println(results);

        String template = """
                당신은 어느 호텔 직원입니다. 문맥에 따라 고객의 질문에 정중하게 답변해 주십시오. 
                컨텍스트가 질문에 대답할 수 없는 경우 '모르겠습니다'라고 대답하세요.
                               
                컨텍스트:
                 {context}
                 질문: 
                 {question}
                 
                 답변:
                """;

        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text(template)
                        .param("context", results)
                        .param("question", question))
                .stream()
                .content();
    }
}
