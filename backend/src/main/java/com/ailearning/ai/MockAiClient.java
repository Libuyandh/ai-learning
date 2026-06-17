package com.ailearning.ai;

import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.Question;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {
    @Override
    public List<AiQuestion> generateQuestions(String content) {
        String topic = content.length() > 18 ? content.substring(0, 18) : content;
        List<AiQuestion> questions = new ArrayList<>();
        questions.add(new AiQuestion("single_choice", topic + "主要考察哪类理解？", List.of("核心概念", "无关常识", "娱乐信息", "随机猜测"), "核心概念", "题目围绕输入内容的核心概念展开。", "核心概念", "medium"));
        questions.add(new AiQuestion("single_choice", "学习材料中最适合先掌握的内容是什么？", List.of("关键定义", "页面颜色", "字体大小", "设备型号"), "关键定义", "关键定义是后续理解的基础。", "关键定义", "easy"));
        questions.add(new AiQuestion("true_false", "闯关学习需要在答题后立即看到解析。", List.of("正确", "错误"), "正确", "即时反馈能帮助用户马上修正理解。", "即时反馈", "easy"));
        questions.add(new AiQuestion("single_choice", "复盘报告最应该帮助用户识别什么？", List.of("薄弱知识点", "手机电量", "天气变化", "页面高度"), "薄弱知识点", "报告的核心价值是总结掌握情况和薄弱点。", "复盘报告", "medium"));
        questions.add(new AiQuestion("true_false", "答错后也应该给出知识讲解。", List.of("正确", "错误"), "正确", "答错时讲解更能帮助理解错因。", "错因讲解", "easy"));
        return questions;
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
