package com.ailearning.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMaterialTextRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
