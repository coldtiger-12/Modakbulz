package com.example.springai2;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import java.util.Arrays;
import java.util.List;

class ChatService {
	private final OpenAiApi openAiApi;
	private final AnthropicApi anthropicApi;
	private final OpenAiApi geminiApi;

	public ChatService(OpenAiApi openAiApi, AnthropicApi anthropicApi, OpenAiApi geminiApi) {
		this.openAiApi = openAiApi;
		this.anthropicApi = anthropicApi;
		this.geminiApi = geminiApi;
	}

	public ChatResponse getOpenAiResponse(String userInput, double temperature) {
		// 메시지 구성
		List<Message> messages = Arrays.asList(
				new SystemMessage("You are a helpful assistant."),
				new UserMessage(userInput)
		);

		// 챗 옵션 설정
		ChatOptions chatOptions = ChatOptions.builder()
				.model("gpt-4o")
				.temperature(temperature)
				.build();

		// 프롬프트 생성
		Prompt prompt = new Prompt(messages, chatOptions);

		// 챗 모델 생성
		OpenAiChatModel chatModel = OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.build();

		// 챗 모델 호출
		return chatModel.call(prompt);
	}

	public ChatResponse getAnthropicResponse(String userInput, double temperature) {
		List<Message> messages = Arrays.asList(
				new SystemMessage("You are a helpful assistant."),
				new UserMessage(userInput)
		);

		ChatOptions chatOptions = ChatOptions.builder()
				.model("claude-3-7-sonnet-20250219")
				.temperature(temperature)
				.build();

		Prompt prompt = new Prompt(messages, chatOptions);

		AnthropicChatModel chatModel = AnthropicChatModel.builder()
				.anthropicApi(anthropicApi)
				.build();

		return chatModel.call(prompt);
	}

	public ChatResponse getGeminiResponse(String userInput, double temperature) {
		// 메시지 구성
		List<Message> messages = Arrays.asList(
				new SystemMessage("You are a helpful assistant."),
				new UserMessage(userInput)
		);

		// 챗 옵션 설정
		ChatOptions chatOptions = ChatOptions.builder()
				.model("gemini-2.0-flash-lite")
				.temperature(temperature)
				.build();

		// 프롬프트 생성
		Prompt prompt = new Prompt(messages, chatOptions);

		// 챗 모델 생성
		OpenAiChatModel chatModel = OpenAiChatModel.builder()
				.openAiApi(geminiApi)
				.build();

		// 챗 모델 호출
		return chatModel.call(prompt);
	}
}

public class Springai2Application {

	public static void main(String[] args) {
		// API 키 설정
		String openAiApiKey = "...";
		String anthropicApiKey = "...";
		String geminiApiKey = "...";
		// API 클라이언트 생성
		OpenAiApi openAiApi = OpenAiApi.builder()
				.apiKey(openAiApiKey)
				.build();

		//AnthropicApi.Builder anthropicApi = new AnthropicApi(anthropicApiKey); // 0.0.9 버전
		AnthropicApi anthropicApi = AnthropicApi.builder()
				.apiKey(anthropicApiKey)
				.build();


		OpenAiApi geminiApi = OpenAiApi.builder()
				.apiKey(geminiApiKey)
				.baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
				.completionsPath("/chat/completions")
				.build();

		// ChatService 생성
		ChatService chatService = new ChatService(openAiApi, anthropicApi, geminiApi);

		// OpenAI 호출
		ChatResponse openAiResponse = chatService.getOpenAiResponse(
				"AI에 대해서 설명해줘.",
				0.5
		);
		System.out.println("OpenAI Response: " + openAiResponse);

		System.out.println("~".repeat(150));
		// Anthropic 호출
		ChatResponse anthropicResponse = chatService.getAnthropicResponse(
				"AI에 대해서 설명해줘.",
				0.5
		);
		System.out.println("Anthropic Response: " + anthropicResponse);
		System.out.println("~".repeat(150));
		// Anthropic 호출
		ChatResponse geminiResponse = chatService.getGeminiResponse(
				"AI에 대해서 설명해줘.",
				0.5
		);
		System.out.println("Gemini Response: " + geminiResponse);
	}
}

