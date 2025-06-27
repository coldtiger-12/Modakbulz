package com.example.springai1;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Springai1Application {

	public static void main(String[] args) {
		SpringApplication.run(Springai1Application.class, args);
	}

	/*
		애플리케이션이 시작된 후 특정 작업을 실행하기 위해 구현됩니다.
		이 인터페이스는 run 메서드를 하나 가지고 있으며, 이 메서드는 애플리케이션이 시작된 후에 호출됩니다.
		주로 데이터베이스 초기화, 설정 데이터 로드, 혹은 프로세스의 일회성 실행 등의 작업에 사용
	*/
	@Bean
	public CommandLineRunner runner(ChatModel model) {
		System.out.println("초기화 과정에서 자동으로 생성된 ChatModel 객체 : " +  model);
		return args -> {
			ChatClient chatClient = ChatClient.builder(model).build();
			System.out.println("생성된 ChatClient 객체 : " +  chatClient);
			String response = chatClient.prompt("스티브 잡스의 명언을 세 개 알려줘")
					.call()
					.content();
			System.out.println("[결과] " + response);
		};
	}
}
