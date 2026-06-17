package com.ailearning.dto;

public record AnswerResponse(boolean correct, String correctAnswer, String explanation, ProgressDto progress) {
}
