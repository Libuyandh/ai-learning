package com.ailearning.dto;

public record AnswerRecordDto(Long id, Long questionId, String userAnswer, Boolean correct, String explanation) {
}
