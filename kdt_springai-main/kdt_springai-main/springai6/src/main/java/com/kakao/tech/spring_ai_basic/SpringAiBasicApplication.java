package com.kakao.tech.spring_ai_basic;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringAiBasicApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiBasicApplication.class, args);
	}

	@Bean
	public ChatClient chatClient(ChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}

}
