package com.ailearning.ai;

import java.util.List;

public record AiQuestion(
        String type,
        String stem,
        List<String> options,
        String correctAnswer,
        String explanation,
        String knowledgePoint,
        String difficulty,
        String sourceType,
        String sourceUrl,
        String evidence,
        Double confidence
) {
}
