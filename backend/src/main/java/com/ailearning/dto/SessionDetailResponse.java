package com.ailearning.dto;

import java.util.List;

public record SessionDetailResponse(
        SessionDto session,
        MaterialDto material,
        List<QuestionDto> questions,
        List<AnswerRecordDto> answers,
        ReportResponse report
) {
}
