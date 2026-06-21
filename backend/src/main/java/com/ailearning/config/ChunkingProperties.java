package com.ailearning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestion.chunking")
public class ChunkingProperties {
    private int targetTokens = 240;
    private int maxTokens = 320;
    private int overlapTokens = 32;

    public int getTargetTokens() {
        return targetTokens;
    }

    public void setTargetTokens(int targetTokens) {
        this.targetTokens = targetTokens;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getOverlapTokens() {
        return overlapTokens;
    }

    public void setOverlapTokens(int overlapTokens) {
        this.overlapTokens = overlapTokens;
    }
}
