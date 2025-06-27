package com.kakao.tech.spring_ai_basic.dto;

import java.util.List;
import java.util.Map;

public record SqlResponse(String sqlQuery, List<Map<String, Object>> results) { }
