package com.ailearning.ai;

import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.Question;
import java.util.List;

public interface AiClient {
    List<AiQuestion> generateQuestions(String content);

    AiReport generateReport(String content, List<Question> questions, List<AnswerRecord> answers);
}
