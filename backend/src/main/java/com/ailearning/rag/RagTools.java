package com.ailearning.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class RagTools {
    private final RagStore ragStore;
    private final ObjectMapper objectMapper;
    private final AtomicInteger callCount = new AtomicInteger();

    public RagTools(RagStore ragStore, ObjectMapper objectMapper) {
        this.ragStore = ragStore;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "从用户已上传的学习资料中进行向量检索。出题前必须优先调用。")
    public String vectorSearch(@ToolParam(description = "出题主题或检索问题") String query) {
        callCount.incrementAndGet();
        List<RagSearchResult> results = ragStore.search(query, 5);
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    public int callCount() {
        return callCount.get();
    }
}
