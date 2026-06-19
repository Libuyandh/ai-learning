package com.ailearning.search;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "mock", matchIfMissing = true)
public class MockWebSearchClient implements WebSearchClient {
    private final AtomicInteger callCount = new AtomicInteger();

    @Override
    public List<WebSearchResult> search(String query) {
        callCount.incrementAndGet();
        if ("__EMPTY__".equals(query)) {
            return List.of();
        }
        return List.of(new WebSearchResult(
                query + " 最新资料",
                "https://example.com/search-result",
                "基于联网搜索返回的资料摘要。",
                query + " 是当前学习主题。生成题目时应仅使用该搜索资料中的事实。"
        ));
    }

    public int callCount() {
        return callCount.get();
    }
}
