package com.kakao.tech.spring_ai_basic.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HackNewsService  {
    private final RestTemplate restTemplate = new RestTemplate();

    //Spring AI가 @Tool의 description이 없을 경우, 코드 자체를 분석하여 도구의 역할을 추론
    //요청(query)을 받아 요약(summary)을 반환하는 뉴스 검색 및 요약 도구이구나"라고 유추
    @Tool
    public Response apply(Request request) {
        String query = request.query();

        List<String> articles = fetchNewsArticles(query);
        String summary = summarizeArticles(articles);

        return new Response(summary);
    }

    private List<String> fetchNewsArticles(String query) {
        System.out.println("Fetching news articles for query: " + query);
        String url = "https://hn.algolia.com/api/v1/search?query={query}&tags=story";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class, query);

        List<String> articles = new ArrayList<>();

        List<Map<String, Object>> hits = (List<Map<String, Object>>) response.get("hits");

        if (hits != null) {
            for (Map<String, Object> hit : hits) {
                String title = (String) hit.get("title");
                String articleUrl = (String) hit.get("url");
                articles.add(title + "\n" + (articleUrl != null ? articleUrl : ""));
            }
        }

        return articles;
    }

    private String summarizeArticles(List<String> articles) {
        if (articles.isEmpty()) {
            return "해당 주제에 대한 기사를 찾을 수 없습니다.";
        }

        StringBuilder content = new StringBuilder("다음은 요청하신 주제에 대한 최신 뉴스입니다:\n");
        for (String article : articles) {
            content.append("- ").append(article).append("\n");
        }
        return content.toString();
    }

    public record Request(String query, Integer limit) {}
    public record Response(String summary) {}
}
