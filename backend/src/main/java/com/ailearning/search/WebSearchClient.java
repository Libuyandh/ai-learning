package com.ailearning.search;

import java.util.List;

public interface WebSearchClient {
    List<WebSearchResult> search(String query);
}
