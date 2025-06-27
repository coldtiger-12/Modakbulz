package com.example.springai11.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class AppConfig {

    @Value("classpath:/prompt.txt")
    private Resource resource;

    @Autowired
    ChatMemory chatMemory;

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder){
         // System Message(LLM에 역할을 부여)
         return chatClientBuilder.build();
         //return chatClientBuilder.defaultSystem("당신은 교육 튜터입니다. 개념을 명확하고 간단하게 설명하세요").build();
         // return chatClientBuilder.defaultSystem(resource).build();
        /*return chatClientBuilder
                .defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build() // chat-memory advisor
                ).build();*/
    }
}
