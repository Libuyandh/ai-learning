package com.ailearning.ai;

import java.util.List;

public record AiReport(String summary, String mastery, List<String> weakPoints, List<String> suggestions) {
}
