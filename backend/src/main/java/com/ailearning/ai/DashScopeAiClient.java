package com.ailearning.ai;

import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.Question;
import com.ailearning.rag.RagSearchResult;
import com.ailearning.rag.RagTools;
import com.ailearning.search.WebSearchResult;
import com.ailearning.search.WebSearchTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final RagTools ragTools;
    private final ObjectMapper objectMapper;

    public DashScopeAiClient(ChatClient.Builder builder, WebSearchTools webSearchTools, RagTools ragTools, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.webSearchTools = webSearchTools;
        this.ragTools = ragTools;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiQuestion> generateQuestions(String content) {
        BeanOutputConverter<AiQuestionResponse> outputConverter = new BeanOutputConverter<>(AiQuestionResponse.class);
        Evidence evidence = evidence(content);
        AiQuestionResponse response = chatClient.prompt()
                .system("""
                        你是严谨的出题老师。
                        只能基于用户上传资料或联网搜索结果出题，不允许使用未经资料支持的模型记忆。
                        每道题必须提供 sourceType（rag 或 web）、来源 URL、直接支持答案的证据片段和 0 到 1 的置信度。
                        证据不足时返回空 questions，不要编造题目。
                        生成 5 到 10 道题；single_choice 至少 4 个选项；true_false 只能有“正确”“错误”两个选项。
                        correctAnswer 必须存在于 options。
                        输出必须符合以下格式：
                        """ + outputConverter.getFormat())
                .user("学习主题或材料：" + content + "\n可用证据：" + evidence.content())
                .tools(ragTools, webSearchTools)
                .call()
                .entity(outputConverter);
        return response == null || response.questions() == null ? List.of() : response.questions().stream()
                .map(question -> fillEvidence(question, evidence))
                .toList();
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

    private Evidence evidence(String content) {
        try {
            List<RagSearchResult> ragResults = objectMapper.readValue(ragTools.vectorSearch(content), new TypeReference<>() {});
            if (!ragResults.isEmpty()) {
                RagSearchResult source = ragResults.get(0);
                return new Evidence("rag", "rag://material/" + source.materialId() + "/chunk/" + source.chunkIndex(), source.content());
            }
        } catch (Exception ignored) {
        }
        try {
            List<WebSearchResult> webResults = objectMapper.readValue(webSearchTools.webSearch(content), new TypeReference<>() {});
            if (!webResults.isEmpty()) {
                WebSearchResult source = webResults.get(0);
                return new Evidence("web", source.url(), source.content());
            }
        } catch (Exception ignored) {
        }
        return new Evidence("", "", "");
    }

    private AiQuestion fillEvidence(AiQuestion question, Evidence evidence) {
        String sourceType = hasText(question.sourceType()) ? question.sourceType() : evidence.sourceType();
        String sourceUrl = hasText(question.sourceUrl()) ? question.sourceUrl() : evidence.sourceUrl();
        String evidenceText = hasText(question.evidence()) ? question.evidence() : evidence.content();
        return new AiQuestion(question.type(), question.stem(), question.options(), question.correctAnswer(),
                question.explanation(), question.knowledgePoint(), question.difficulty(),
                sourceType, sourceUrl, evidenceText, question.confidence());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record Evidence(String sourceType, String sourceUrl, String content) {
    }
}
