package com.ailearning.service;

import com.ailearning.ai.AiClient;
import com.ailearning.ai.AiQuestion;
import com.ailearning.ai.AiReport;
import com.ailearning.common.BusinessException;
import com.ailearning.domain.AnswerRecord;
import com.ailearning.domain.LearningReport;
import com.ailearning.domain.LearningSession;
import com.ailearning.domain.Question;
import com.ailearning.domain.SourceMaterial;
import com.ailearning.dto.AnswerRecordDto;
import com.ailearning.dto.AnswerResponse;
import com.ailearning.dto.CreateSessionRequest;
import com.ailearning.dto.CreateSessionResponse;
import com.ailearning.dto.MaterialDto;
import com.ailearning.dto.ProgressDto;
import com.ailearning.dto.QuestionDto;
import com.ailearning.dto.QuestionsResponse;
import com.ailearning.dto.ReportResponse;
import com.ailearning.dto.SessionDetailResponse;
import com.ailearning.dto.SessionDto;
import com.ailearning.dto.SubmitAnswerRequest;
import com.ailearning.dto.WrongQuestionDto;
import com.ailearning.mapper.AnswerRecordMapper;
import com.ailearning.mapper.LearningReportMapper;
import com.ailearning.mapper.LearningSessionMapper;
import com.ailearning.mapper.QuestionMapper;
import com.ailearning.mapper.SourceMaterialMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LearningService {
    private final LearningSessionMapper sessionMapper;
    private final SourceMaterialMapper materialMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerMapper;
    private final LearningReportMapper reportMapper;
    private final AiClient aiClient;
    private final JsonSupport jsonSupport;

    public LearningService(
            LearningSessionMapper sessionMapper,
            SourceMaterialMapper materialMapper,
            QuestionMapper questionMapper,
            AnswerRecordMapper answerMapper,
            LearningReportMapper reportMapper,
            AiClient aiClient,
            JsonSupport jsonSupport
    ) {
        this.sessionMapper = sessionMapper;
        this.materialMapper = materialMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.reportMapper = reportMapper;
        this.aiClient = aiClient;
        this.jsonSupport = jsonSupport;
    }

    @Transactional
    public CreateSessionResponse  createSession(CreateSessionRequest request) {
        if ("text".equals(request.inputType()) && !StringUtils.hasText(request.content())) {
            throw new BusinessException("VALIDATION_ERROR", "content 不能为空", HttpStatus.BAD_REQUEST);
        }
        if (!"text".equals(request.inputType())) {
            throw new BusinessException("UNSUPPORTED_INPUT", "首版仅支持文本输入", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        LearningSession session = new LearningSession();
        session.setTitle(titleOf(request.content()));
        session.setInputType(request.inputType());
        session.setStatus("CREATED");
        session.setQuestionCount(0);
        session.setCorrectCount(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);

        SourceMaterial material = new SourceMaterial();
        material.setSessionId(session.getId());
        material.setType(request.inputType());
        material.setContent(request.content());
        material.setParsedText(request.content());
        material.setCreatedAt(now);
        materialMapper.insert(material);

        return new CreateSessionResponse(session.getId(), session.getStatus());
    }

    @Transactional
    public QuestionsResponse generateQuestions(Long sessionId) {
        LearningSession session = requireSession(sessionId);
        List<Question> existing = questionsOf(sessionId);
        if (!existing.isEmpty()) {
            return new QuestionsResponse(sessionId, existing.stream().map(this::toQuestionDto).toList());
        }

        SourceMaterial material = requireMaterial(sessionId);
        List<AiQuestion> generated = generateWithRetry(material.getParsedText());
        validateQuestions(generated);

        LocalDateTime now = LocalDateTime.now();
        List<Question> saved = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            AiQuestion aiQuestion = generated.get(i);
            Question question = new Question();
            question.setSessionId(sessionId);
            question.setQuestionType(aiQuestion.type());
            question.setStem(aiQuestion.stem());
            question.setOptionsJson(jsonSupport.write(aiQuestion.options()));
            question.setCorrectAnswer(aiQuestion.correctAnswer());
            question.setExplanation(aiQuestion.explanation());
            question.setKnowledgePoint(aiQuestion.knowledgePoint());
            question.setDifficulty(aiQuestion.difficulty());
            question.setSourceType(aiQuestion.sourceType());
            question.setSourceUrl(aiQuestion.sourceUrl());
            question.setEvidenceText(aiQuestion.evidence());
            question.setConfidence(aiQuestion.confidence());
            question.setSortOrder(i + 1);
            question.setCreatedAt(now);
            questionMapper.insert(question);
            saved.add(question);
        }

        session.setStatus("QUESTION_READY");
        session.setQuestionCount(saved.size());
        session.setUpdatedAt(now);
        sessionMapper.updateById(session);

        return new QuestionsResponse(sessionId, saved.stream().map(this::toQuestionDto).toList());
    }

    @Transactional
    public AnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        LearningSession session = requireSession(sessionId);
        Question question = questionMapper.selectById(request.questionId());
        if (question == null || !sessionId.equals(question.getSessionId())) {
            throw new BusinessException("QUESTION_NOT_FOUND", "题目不存在", HttpStatus.NOT_FOUND);
        }
        Long count = answerMapper.selectCount(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getSessionId, sessionId)
                .eq(AnswerRecord::getQuestionId, request.questionId()));
        if (count > 0) {
            throw new BusinessException("ANSWER_EXISTS", "该题已提交", HttpStatus.CONFLICT);
        }

        boolean correct = question.getCorrectAnswer().equals(request.userAnswer());
        AnswerRecord record = new AnswerRecord();
        record.setSessionId(sessionId);
        record.setQuestionId(request.questionId());
        record.setUserAnswer(request.userAnswer());
        record.setCorrect(correct);
        record.setExplanation(question.getExplanation());
        record.setAnsweredAt(LocalDateTime.now());
        answerMapper.insert(record);

        int answeredCount = answersOf(sessionId).size();
        int correctCount = (int) answersOf(sessionId).stream().filter(AnswerRecord::getCorrect).count();
        session.setCorrectCount(correctCount);
        session.setStatus(answeredCount >= session.getQuestionCount() && session.getQuestionCount() > 0 ? "ANSWERED" : "ANSWERING");
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return new AnswerResponse(correct, question.getCorrectAnswer(), question.getExplanation(),
                new ProgressDto(answeredCount, session.getQuestionCount(), correctCount));
    }

    @Transactional
    public ReportResponse generateReport(Long sessionId) {
        LearningSession session = requireSession(sessionId);
        LearningReport existing = reportOf(sessionId);
        if (existing != null) {
            return toReportResponse(existing);
        }

        List<Question> questions = questionsOf(sessionId);
        if (questions.isEmpty()) {
            throw new BusinessException("QUESTION_REQUIRED", "请先生成题目", HttpStatus.BAD_REQUEST);
        }
        List<AnswerRecord> answers = answersOf(sessionId);
        if (answers.isEmpty()) {
            throw new BusinessException("ANSWER_REQUIRED", "请先提交答案", HttpStatus.BAD_REQUEST);
        }

        SourceMaterial material = requireMaterial(sessionId);
        AiReport aiReport = aiClient.generateReport(material.getParsedText(), questions, answers);
        int correctCount = (int) answers.stream().filter(AnswerRecord::getCorrect).count();
        BigDecimal accuracy = BigDecimal.valueOf(correctCount * 100.0 / questions.size()).setScale(2, RoundingMode.HALF_UP);
        int score = accuracy.setScale(0, RoundingMode.HALF_UP).intValue();
        List<WrongQuestionDto> wrongQuestions = wrongQuestions(questions, answers);

        LearningReport report = new LearningReport();
        report.setSessionId(sessionId);
        report.setSummary(aiReport.summary());
        report.setMastery(aiReport.mastery());
        report.setScore(score);
        report.setAccuracy(accuracy);
        report.setWeakPointsJson(jsonSupport.write(aiReport.weakPoints()));
        report.setSuggestionsJson(jsonSupport.write(aiReport.suggestions()));
        report.setWrongQuestionsJson(jsonSupport.write(wrongQuestions));
        report.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);

        session.setStatus("REPORTED");
        session.setCorrectCount(correctCount);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return toReportResponse(report);
    }

    public SessionDetailResponse getSession(Long sessionId) {
        LearningSession session = requireSession(sessionId);
        SourceMaterial material = requireMaterial(sessionId);
        LearningReport report = reportOf(sessionId);
        return new SessionDetailResponse(
                new SessionDto(session.getId(), session.getTitle(), session.getInputType(), session.getStatus(), session.getQuestionCount(), session.getCorrectCount()),
                new MaterialDto(material.getId(), material.getType(), material.getContent(), material.getParsedText()),
                questionsOf(sessionId).stream().map(this::toQuestionDto).toList(),
                answersOf(sessionId).stream().map(answer -> new AnswerRecordDto(answer.getId(), answer.getQuestionId(), answer.getUserAnswer(), answer.getCorrect(), answer.getExplanation())).toList(),
                report == null ? null : toReportResponse(report)
        );
    }

    private List<AiQuestion> generateWithRetry(String content) {
        RuntimeException last = null;
        for (int i = 0; i < 2; i++) {
            try {
                List<AiQuestion> questions = aiClient.generateQuestions(content);
                validateQuestions(questions);
                return questions;
            } catch (RuntimeException exception) {
                last = exception;
            }
        }
        throw new BusinessException("AI_GENERATION_FAILED", last == null ? "生成失败" : last.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    private void validateQuestions(List<AiQuestion> questions) {
        if (questions == null || questions.size() < 5 || questions.size() > 10) {
            throw new IllegalArgumentException("题目数量必须为 5 到 10 道");
        }
        for (AiQuestion question : questions) {
            if (!StringUtils.hasText(question.stem()) || !StringUtils.hasText(question.correctAnswer()) || !StringUtils.hasText(question.explanation())) {
                throw new IllegalArgumentException("题目字段不完整");
            }
            if (question.options() == null || question.options().size() < 2 || !question.options().contains(question.correctAnswer())) {
                throw new IllegalArgumentException("选项不合法");
            }
            if ("single_choice".equals(question.type()) && question.options().size() < 4) {
                throw new IllegalArgumentException("单选题至少 4 个选项");
            }
            if (!StringUtils.hasText(question.sourceType()) || !List.of("rag", "web").contains(question.sourceType())) {
                throw new IllegalArgumentException("题目来源类型不合法");
            }
            if (!StringUtils.hasText(question.sourceUrl()) || !StringUtils.hasText(question.evidence())) {
                throw new IllegalArgumentException("题目缺少来源或证据");
            }
            if (question.confidence() == null || question.confidence() < 0.6 || question.confidence() > 1) {
                throw new IllegalArgumentException("题目置信度不合格");
            }
        }
    }

    private LearningSession requireSession(Long sessionId) {
        LearningSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("SESSION_NOT_FOUND", "学习会话不存在", HttpStatus.NOT_FOUND);
        }
        return session;
    }

    private SourceMaterial requireMaterial(Long sessionId) {
        SourceMaterial material = materialMapper.selectOne(new LambdaQueryWrapper<SourceMaterial>().eq(SourceMaterial::getSessionId, sessionId));
        if (material == null) {
            throw new BusinessException("MATERIAL_NOT_FOUND", "学习材料不存在", HttpStatus.NOT_FOUND);
        }
        return material;
    }

    private List<Question> questionsOf(Long sessionId) {
        return questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getSessionId, sessionId)
                .orderByAsc(Question::getSortOrder));
    }

    private List<AnswerRecord> answersOf(Long sessionId) {
        return answerMapper.selectList(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getSessionId, sessionId)
                .orderByAsc(AnswerRecord::getId));
    }

    private LearningReport reportOf(Long sessionId) {
        return reportMapper.selectOne(new LambdaQueryWrapper<LearningReport>().eq(LearningReport::getSessionId, sessionId));
    }

    private QuestionDto toQuestionDto(Question question) {
        return new QuestionDto(question.getId(), question.getQuestionType(), question.getStem(), jsonSupport.readStringList(question.getOptionsJson()),
                question.getCorrectAnswer(), question.getExplanation(), question.getKnowledgePoint(), question.getDifficulty(),
                question.getSourceType(), question.getSourceUrl(), question.getEvidenceText(), question.getConfidence(), question.getSortOrder());
    }

    private ReportResponse toReportResponse(LearningReport report) {
        return new ReportResponse(report.getId(), report.getSummary(), report.getMastery(), report.getScore(), report.getAccuracy(),
                jsonSupport.readStringList(report.getWeakPointsJson()),
                jsonSupport.readStringList(report.getSuggestionsJson()),
                jsonSupport.readWrongQuestions(report.getWrongQuestionsJson()));
    }

    private List<WrongQuestionDto> wrongQuestions(List<Question> questions, List<AnswerRecord> answers) {
        return answers.stream()
                .filter(answer -> !Boolean.TRUE.equals(answer.getCorrect()))
                .map(answer -> {
                    Question question = questions.stream()
                            .filter(item -> item.getId().equals(answer.getQuestionId()))
                            .findFirst()
                            .orElseThrow();
                    return new WrongQuestionDto(question.getId(), question.getStem(), answer.getUserAnswer(), question.getCorrectAnswer(), question.getExplanation(), question.getKnowledgePoint());
                })
                .sorted(Comparator.comparing(WrongQuestionDto::questionId))
                .toList();
    }

    private String titleOf(String content) {
        String trimmed = content == null ? "未命名学习" : content.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }
}
