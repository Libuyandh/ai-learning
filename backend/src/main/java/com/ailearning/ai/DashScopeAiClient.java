package com.ailearning.ai;

import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "dashscope")
public class DashScopeAiClient implements AiClient {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public DashScopeAiClient(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiQuestion> generateQuestions(String content) {
        String result = chatClient.prompt()
                .system("""
                        你是严谨的出题老师。只返回 JSON，不要 Markdown。
                        JSON 格式：
                        {"questions":[{"type":"single_choice|true_false","stem":"题干","options":["选项"],"correctAnswer":"正确答案","explanation":"解析","knowledgePoint":"知识点","difficulty":"easy|medium|hard"}]}
                        要求：生成 5 到 10 道题；single_choice 至少 4 个选项；true_false 只能有“正确”“错误”两个选项；correctAnswer 必须存在于 options。
                        """)
                .user("学习材料：" + content)
                .call()
                .content();
        try {
            return objectMapper.readValue(result, AiQuestionResponse.class).questions();
        } catch (Exception exception) {
            throw new IllegalStateException("AI 题目 JSON 解析失败", exception);
        }
    }

    @Override
    public AiReport generateReport(String content, List<Question> questions, List<AnswerRecord> answers) {
        String result = chatClient.prompt()
                .system("""
                        你是学习复盘教练。只返回 JSON，不要 Markdown。
                        JSON 格式：
                        {"summary":"学习摘要","mastery":"掌握情况","weakPoints":["薄弱知识点"],"suggestions":["学习建议"]}
                        """)
                .user("学习材料：" + content + "\n题目：" + questions + "\n答题记录：" + answers)
                .call()
                .content();
        try {
            return objectMapper.readValue(result, AiReport.class);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 报告 JSON 解析失败", exception);
        }
    }
}
