package com.kakao.tech.spring_ai_basic.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;

public class DateTimeTools {

    //AI 모델이 이 기능이 무엇을 하는지 이해하는 데 결정적인 역할을 합니다.
    // AI는 이 설명을 보고 사용자의 질문에 이 기능이 필요한지 판단합
    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

}