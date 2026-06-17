package com.ailearning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateSessionRequest(
        @NotBlank @Pattern(regexp = "text|url|file|video") String inputType,
        String content,
        Long fileId
) {
}
