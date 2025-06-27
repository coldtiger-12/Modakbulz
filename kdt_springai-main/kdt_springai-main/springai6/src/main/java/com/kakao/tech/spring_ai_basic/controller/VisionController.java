package com.kakao.tech.spring_ai_basic.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("vision")
public class VisionController {

    private ChatClient chatClient;

    public VisionController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping
    public String visionPage() {
        return "vision";
    }

    @PostMapping("upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> vision(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("message") String message) {
        Map<String, String> response = new HashMap<>();
        try {
            // 사용자 메시지와 함께 처리 (기본값은 "이미지 분석해줘")
            String userMessage = (message == null || message.isEmpty()) ? "이미지 분석해줘" : message;

            String result = chatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(userMessage)
                            .media(MimeType.valueOf(Objects.requireNonNull(file.getContentType())), file.getResource()))
                    .call()
                    .content();

            response.put("result", result);  // 분석 결과 반환
            return ResponseEntity.ok(response); // JSON 응답
        } catch (MaxUploadSizeExceededException e) {
            response.put("error", "업로드한 파일이 너무 큽니다. 최대 10MB 파일만 업로드 가능합니다.");
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
        } catch (Exception e) {
            response.put("error", "파일 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
