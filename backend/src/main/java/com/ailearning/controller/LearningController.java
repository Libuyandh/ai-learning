package com.ailearning.controller;

import com.ailearning.common.ApiResponse;
import com.ailearning.dto.AnswerResponse;
import com.ailearning.dto.CreateSessionRequest;
import com.ailearning.dto.CreateSessionResponse;
import com.ailearning.dto.QuestionsResponse;
import com.ailearning.dto.ReportResponse;
import com.ailearning.dto.SessionDetailResponse;
import com.ailearning.dto.SubmitAnswerRequest;
import com.ailearning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/sessions")
public class LearningController {
    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping
    public ApiResponse<CreateSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(learningService.createSession(request));
    }

    @PostMapping("/{sessionId}/questions")
    public ApiResponse<QuestionsResponse> generateQuestions(@PathVariable Long sessionId) {
        return ApiResponse.ok(learningService.generateQuestions(sessionId));
    }

    @PostMapping("/{sessionId}/answers")
    public ApiResponse<AnswerResponse> submitAnswer(@PathVariable Long sessionId, @Valid @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.ok(learningService.submitAnswer(sessionId, request));
    }

    @PostMapping("/{sessionId}/report")
    public ApiResponse<ReportResponse> generateReport(@PathVariable Long sessionId) {
        return ApiResponse.ok(learningService.generateReport(sessionId));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionDetailResponse> getSession(@PathVariable Long sessionId) {
        return ApiResponse.ok(learningService.getSession(sessionId));
    }
}
