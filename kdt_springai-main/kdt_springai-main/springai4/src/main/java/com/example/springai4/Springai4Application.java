package com.example.springai4;

import com.example.springai4.vo.MathReasoning;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
//import org.springframework.ai.model.Media;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

@SpringBootApplication
public class Springai4Application {

	public static void main(String[] args) {
		SpringApplication.run(Springai4Application.class, args);
	}

	@Bean
	public CommandLineRunner runner1(ChatClient.Builder builder) {
		System.out.println("runner1 : " + builder);
		return args -> {
			ChatClient chatClient = builder.build();
			String response = chatClient.prompt("스티브잡스의 명언을 한 개 알려줘")
					.call()
					.content();
			System.out.println("-".repeat(100));
			System.out.println("[ 결과1 ] " + response);
		};
	}
	@Bean
	public CommandLineRunner runner2(ChatModel chatModel) {
		System.out.println("runner2 : " + chatModel);
		return args -> {
			var imageResource = new ClassPathResource("/static/images/dogcat.jpg");
			System.out.println(imageResource.contentLength());
			/*var userMessage = new UserMessage("이 사진에 대해서 설명해줘",
					new Media(MimeTypeUtils.IMAGE_JPEG,  imageResource));*/

			var userMessage = UserMessage.builder().text("이 사진에 대해서 설명해줘").
					media(new Media(MimeTypeUtils.IMAGE_JPEG,  imageResource)).build();

			ChatResponse response = chatModel.call(new Prompt(userMessage,
					ChatOptions.builder().model(String.valueOf(OllamaModel.LLAVA)).build()));
			System.out.println("-".repeat(100));
			System.out.println("[ 결과2 ] " + response.getResult().getOutput().getText());
		};
	}
	@Bean
	public CommandLineRunner runner3(OllamaChatModel ollamaChatModel) {
		System.out.println("runner3 : " + ollamaChatModel);
		return args -> {
			var outputConverter = new BeanOutputConverter<>(MathReasoning.class);

			Prompt prompt = new Prompt("how can I solve 8x + 7 = -23",
					OllamaOptions.builder()
							.model(OllamaModel.LLAMA3_2_VISION_11b.getName())
							.format(outputConverter.getJsonSchemaMap())
							.build());

			ChatResponse response = ollamaChatModel.call(prompt);
			String content = response.getResult().getOutput().getText();

			MathReasoning mathReasoning = outputConverter.convert(content);
			System.out.println("-".repeat(100));
			System.out.println("[ 결과3 ] " + mathReasoning.finalAnswer());
			System.out.println("[ 결과3 ] " + mathReasoning.steps());
			System.out.println("[ 결과3 ] " + mathReasoning);
		};
	}

}
