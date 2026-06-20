package com.ailearning.dto;

public record MaterialResponse(
        Long materialId,
        String title,
        String type,
        String status,
        Integer chunkCount
) {
}
