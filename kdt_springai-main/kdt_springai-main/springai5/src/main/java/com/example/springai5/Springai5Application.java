package com.example.springai5;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Springai5Application {

	public static void main(String[] args) {
		SpringApplication.run(Springai5Application.class, args);
	}

	@Bean
	public ChatClient openAIChatClient(OpenAiChatModel chatModel) {
		return ChatClient.create(chatModel);
	}

	@Bean
	public ChatClient ollamaChatClient(OllamaChatModel chatModel) {
		return ChatClient.create(chatModel);
	}

}
