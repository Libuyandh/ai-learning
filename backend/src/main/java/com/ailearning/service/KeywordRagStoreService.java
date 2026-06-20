package com.ailearning.service;

import com.ailearning.domain.MaterialChunk;
import com.ailearning.mapper.MaterialChunkMapper;
import com.ailearning.rag.RagSearchResult;
import com.ailearning.rag.RagStore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "rag.provider", havingValue = "keyword", matchIfMissing = true)
public class KeywordRagStoreService implements RagStore {
    private final MaterialChunkMapper chunkMapper;

    public KeywordRagStoreService(MaterialChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    @Override
    public void add(Long materialId, List<String> chunks) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < chunks.size(); i++) {
            MaterialChunk chunk = new MaterialChunk();
            chunk.setMaterialId(materialId);
            chunk.setChunkIndex(i + 1);
            chunk.setContent(chunks.get(i));
            chunk.setCreatedAt(now);
            chunkMapper.insert(chunk);
        }
    }

    @Override
    public List<RagSearchResult> search(String query, int topK) {
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty()) {
            return List.of();
        }
        return chunkMapper.selectList(new LambdaQueryWrapper<MaterialChunk>().orderByDesc(MaterialChunk::getId))
                .stream()
                .map(chunk -> new RagSearchResult(chunk.getMaterialId(), chunk.getChunkIndex(), chunk.getContent(), score(query, queryTerms, chunk.getContent())))
                .filter(result -> result.score() > 0)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(topK)
                .toList();
    }

    private Double score(String query, Set<String> queryTerms, String content) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedContent = content == null ? "" : content.toLowerCase();
        if (normalizedQuery.length() >= 2 && normalizedContent.contains(normalizedQuery)) {
            return 1.0;
        }
        Set<String> contentTerms = terms(content);
        long hit = queryTerms.stream().filter(contentTerms::contains).count();
        return hit == 0 ? 0 : hit * 1.0 / queryTerms.size();
    }

    private Set<String> terms(String value) {
        String normalized = value == null ? "" : value.toLowerCase().replaceAll("[^\\p{IsHan}\\p{IsAlphabetic}\\p{IsDigit}]+", " ");
        Set<String> terms = new HashSet<>(Arrays.stream(normalized.split("\\s+")).filter(item -> item.length() >= 2).toList());
        if (terms.isEmpty() && normalized.trim().length() >= 2) {
            terms.add(normalized.trim());
        }
        return terms;
    }
}
