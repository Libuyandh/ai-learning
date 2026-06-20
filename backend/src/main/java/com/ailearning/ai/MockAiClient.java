package com.ailearning.ai;

import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.Question;
import com.ailearning.rag.RagSearchResult;
import com.ailearning.rag.RagTools;
import com.ailearning.search.WebSearchResult;
import com.ailearning.search.WebSearchTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {
    private final WebSearchTools webSearchTools;
    private final RagTools ragTools;
    private final ObjectMapper objectMapper;

    public MockAiClient(WebSearchTools webSearchTools, RagTools ragTools, ObjectMapper objectMapper) {
        this.webSearchTools = webSearchTools;
        this.ragTools = ragTools;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiQuestion> generateQuestions(String content) {
        List<RagSearchResult> ragSources;
        try {
            ragSources = objectMapper.readValue(ragTools.vectorSearch(content), new TypeReference<>() {});
        } catch (Exception exception) {
            ragSources = List.of();
        }
        if (!ragSources.isEmpty()) {
            RagSearchResult source = ragSources.get(0);
            String sourceUrl = "rag://material/" + source.materialId() + "/chunk/" + source.chunkIndex();
            return questions(content, "rag", sourceUrl, source.content());
        }

        List<WebSearchResult> sources;
        try {
            sources = objectMapper.readValue(webSearchTools.webSearch(content), new TypeReference<>() {});
        } catch (Exception exception) {
            sources = List.of();
        }
        if (sources.isEmpty()) {
            return List.of();
        }
        WebSearchResult source = sources.get(0);
        return questions(content, "web", source.url(), source.content());
    }

    private List<AiQuestion> questions(String content, String sourceType, String sourceUrl, String evidence) {
        String topic = content.length() > 18 ? content.substring(0, 18) : content;
        List<AiQuestion> questions = new ArrayList<>();
        questions.add(question("single_choice", topic + "主要考察哪类理解？", List.of("核心概念", "无关常识", "娱乐信息", "随机猜测"), "核心概念", "题目围绕资料证据的核心概念展开。", "核心概念", "medium", sourceType, sourceUrl, evidence));
        questions.add(question("single_choice", "学习材料中最适合先掌握的内容是什么？", List.of("关键定义", "页面颜色", "字体大小", "设备型号"), "关键定义", "关键定义是后续理解的基础。", "关键定义", "easy", sourceType, sourceUrl, evidence));
        questions.add(question("true_false", "生成题目前应先检索用户上传资料。", List.of("正确", "错误"), "正确", "Agentic RAG 要先使用本地资料，再按需联网搜索。", "Agentic RAG", "easy", sourceType, sourceUrl, evidence));
        questions.add(question("single_choice", "复盘报告最应该帮助用户识别什么？", List.of("薄弱知识点", "手机电量", "天气变化", "页面高度"), "薄弱知识点", "报告的核心价值是总结掌握情况和薄弱点。", "复盘报告", "medium", sourceType, sourceUrl, evidence));
        questions.add(question("true_false", "没有资料证据时仍应强行生成题目。", List.of("正确", "错误"), "错误", "证据不足时不应生成可能包含幻觉的题目。", "证据约束", "easy", sourceType, sourceUrl, evidence));
        return questions;
    }

    private AiQuestion question(String type, String stem, List<String> options, String correctAnswer,
                                String explanation, String knowledgePoint, String difficulty,
                                String sourceType, String sourceUrl, String evidence) {
        return new AiQuestion(type, stem, options, correctAnswer, explanation, knowledgePoint, difficulty,
                sourceType, sourceUrl, evidence, 0.9);
    }

    @Override
    public AiReport generateReport(String content, List<Question> questions, List<AnswerRecord> answers) {
        List<String> weakPoints = answers.stream()
                .filter(answer -> !Boolean.TRUE.equals(answer.getCorrect()))
                .map(answer -> questions.stream()
                        .filter(question -> question.getId().equals(answer.getQuestionId()))
                        .findFirst()
                        .map(Question::getKnowledgePoint)
                        .orElse("核心概念"))
                .distinct()
                .toList();
        List<String> finalWeakPoints = weakPoints.isEmpty() ? List.of("继续保持当前掌握水平") : weakPoints;
        return new AiReport("本次学习围绕输入内容完成了核心概念自测。", "已完成基础闯关，可根据错题继续复习。", finalWeakPoints, List.of("回看错题解析", "围绕薄弱点再做一次短练习"));
    }
}
