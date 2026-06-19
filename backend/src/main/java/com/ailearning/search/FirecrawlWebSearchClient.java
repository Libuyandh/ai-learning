package com.ailearning.search;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "search.provider", havingValue = "firecrawl")
public class FirecrawlWebSearchClient implements WebSearchClient {
    private final RestClient restClient;
    private final int maxResults;
    private final int maxContentLength;

    public FirecrawlWebSearchClient(
            RestClient.Builder builder,
            @Value("${search.api-key:}") String apiKey,
            @Value("${search.max-results:5}") int maxResults,
            @Value("${search.max-content-length:12000}") int maxContentLength
    ) {
        this.restClient = builder
                .baseUrl("https://api.firecrawl.dev/v2")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.maxResults = maxResults;
        this.maxContentLength = maxContentLength;
    }

    @Override
    public List<WebSearchResult> search(String query) {
        try {
            JsonNode response = restClient.post()
                    .uri("/search")
                    .body(Map.of(
                            "query", query,
                            "limit", maxResults,
                            "scrapeOptions", Map.of("formats", List.of("markdown"))
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode webResults = response == null ? null : response.path("data").path("web");
            if (webResults == null || !webResults.isArray()) {
                return List.of();
            }
            List<WebSearchResult> results = new ArrayList<>();
            for (JsonNode item : webResults) {
                String content = text(item, "markdown");
                if (content.length() > maxContentLength) {
                    content = content.substring(0, maxContentLength);
                }
                results.add(new WebSearchResult(
                        text(item, "title"),
                        text(item, "url"),
                        text(item, "description"),
                        content
                ));
            }
            return results;
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return StringUtils.hasText(value) ? value : "";
    }
}
