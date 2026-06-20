package com.ailearning.rag;

import java.util.List;

public interface RagStore {
    void add(Long materialId, List<String> chunks);

    List<RagSearchResult> search(String query, int topK);
}
