package com.ailearning.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LearningReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String summary;
    private String mastery;
    private Integer score;
    private BigDecimal accuracy;
    private String weakPointsJson;
    private String suggestionsJson;
    private String wrongQuestionsJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getMastery() { return mastery; }
    public void setMastery(String mastery) { this.mastery = mastery; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public BigDecimal getAccuracy() { return accuracy; }
    public void setAccuracy(BigDecimal accuracy) { this.accuracy = accuracy; }
    public String getWeakPointsJson() { return weakPointsJson; }
    public void setWeakPointsJson(String weakPointsJson) { this.weakPointsJson = weakPointsJson; }
    public String getSuggestionsJson() { return suggestionsJson; }
    public void setSuggestionsJson(String suggestionsJson) { this.suggestionsJson = suggestionsJson; }
    public String getWrongQuestionsJson() { return wrongQuestionsJson; }
    public void setWrongQuestionsJson(String wrongQuestionsJson) { this.wrongQuestionsJson = wrongQuestionsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
