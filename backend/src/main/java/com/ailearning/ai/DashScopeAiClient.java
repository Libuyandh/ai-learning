package com.ailearning.ai;

import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.Question;
import com.ailearning.search.WebSearchTools;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "dashscope")
public class DashScopeAiClient implements AiClient {
    private final ChatClient chatClient;
    private final WebSearchTools webSearchTools;

    public DashScopeAiClient(ChatClient.Builder builder, WebSearchTools webSearchTools) {
        this.chatClient = builder.build();
        this.webSearchTools = webSearchTools;
    }

    @Override
    public List<AiQuestion> generateQuestions(String content) {
        BeanOutputConverter<AiQuestionResponse> outputConverter = new BeanOutputConverter<>(AiQuestionResponse.class);
        AiQuestionResponse response = chatClient.prompt()
                .system("""
                        你是严谨的出题老师。
                        遇到新知识、冷门概念、多义词或时效性内容时，必须先调用 webSearch。
                        只能基于用户输入和 webSearch 返回的资料出题，不允许使用未经资料支持的模型记忆。
                        每道题必须提供来源 URL、直接支持答案的证据片段和 0 到 1 的置信度。
                        证据不足时返回空 questions，不要编造题目。
                        生成 5 到 10 道题；single_choice 至少 4 个选项；true_false 只能有“正确”“错误”两个选项；
                        correctAnswer 必须存在于 options。
                        输出必须符合以下格式：
                        """ + outputConverter.getFormat())
                .user("学习主题或材料：" + content)
                .tools(webSearchTools)
                .call()
                .entity(outputConverter);
        return response == null || response.questions() == null ? List.of() : response.questions();
    }

    @Override
    public AiReport generateReport(String content, List<Question> questions, List<AnswerRecord> answers) {
        BeanOutputConverter<AiReport> outputConverter = new BeanOutputConverter<>(AiReport.class);
        AiReport report = chatClient.prompt()
                .system("你是学习复盘教练。输出必须符合以下格式：\n" + outputConverter.getFormat())
                .user("学习材料：" + content + "\n题目：" + questions + "\n答题记录：" + answers)
                .call()
                .entity(outputConverter);
        if (report == null) {
            throw new IllegalStateException("AI 报告生成失败");
        }
        return report;
    }
}
