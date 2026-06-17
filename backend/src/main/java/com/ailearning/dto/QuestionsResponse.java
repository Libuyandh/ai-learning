package com.ailearning.dto;

import java.util.List;

public record QuestionsResponse(Long sessionId, List<QuestionDto> questions) {
}
