package com.ailearning.rag;

public record RagSearchResult(
        Long materialId,
        Integer chunkIndex,
        String content,
        Double score
) {
}
