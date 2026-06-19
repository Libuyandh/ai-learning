package com.ailearning.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTools {
    private final WebSearchClient webSearchClient;
    private final ObjectMapper objectMapper;

    public WebSearchTools(WebSearchClient webSearchClient, ObjectMapper objectMapper) {
        this.webSearchClient = webSearchClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "搜索互联网中的最新资料。遇到新知识、冷门概念、多义词或时效性内容时必须调用。")
    public String webSearch(@ToolParam(description = "完整、明确的搜索关键词") String query) {
        List<WebSearchResult> results = webSearchClient.search(query);
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
