package com.ailearning.dto;

public record WrongQuestionDto(Long questionId, String stem, String userAnswer, String correctAnswer, String explanation, String knowledgePoint) {
}
