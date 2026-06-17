package com.ailearning.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportResponse(
        Long reportId,
        String summary,
        String mastery,
        Integer score,
        BigDecimal accuracy,
        List<String> weakPoints,
        List<String> suggestions,
        List<WrongQuestionDto> wrongQuestions
) {
}
