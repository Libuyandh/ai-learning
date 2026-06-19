package com.ailearning.dto;

import java.util.List;

public record QuestionDto(
        Long id,
        String type,
        String stem,
        List<String> options,
        String correctAnswer,
        String explanation,
        String knowledgePoint,
        String difficulty,
        String sourceUrl,
        String evidence,
        Double confidence,
        Integer sortOrder
) {
}
