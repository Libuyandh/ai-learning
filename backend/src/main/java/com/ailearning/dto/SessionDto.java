package com.ailearning.dto;

public record SessionDto(Long sessionId, String title, String inputType, String status, Integer questionCount, Integer correctCount) {
}
