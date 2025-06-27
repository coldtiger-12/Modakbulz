package com.kakao.tech.spring_ai_basic.controller;

import com.kakao.tech.spring_ai_basic.dto.SqlResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@RestController
@RequestMapping("sql")
public class SqlController {
    @Value("classpath:/schema.sql")
    private Resource ddlResource;

    @Value("classpath:/sql-prompt-template.st")
    private Resource sqlPromptTemplateResource;

    private final ChatClient aiClient;
    private final JdbcTemplate jdbcTemplate;

    public SqlController(
            ChatClient.Builder aiClientBuilder,
            JdbcTemplate jdbcTemplate) {
        this.aiClient = aiClientBuilder.build();
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public SqlResponse sql(@RequestParam(name = "q") String question) throws IOException {
        String schema = ddlResource.getContentAsString(Charset.defaultCharset());

        String query = aiClient.prompt()
                .user(userSpec -> userSpec
                        .text(sqlPromptTemplateResource)
                        .param("question", question)
                        .param("ddl", schema)
                )
                .call()
                .content();

        if (query.toLowerCase().startsWith("select")) {
            return new SqlResponse(query, jdbcTemplate.queryForList(query));
        }

        return new SqlResponse(query, List.of());
    }
}
