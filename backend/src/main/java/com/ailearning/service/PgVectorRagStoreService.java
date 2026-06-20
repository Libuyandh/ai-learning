package com.ailearning.service;

import com.ailearning.domain.MaterialChunk;
import com.ailearning.mapper.MaterialChunkMapper;
import com.ailearning.rag.RagSearchResult;
import com.ailearning.rag.RagStore;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "rag.provider", havingValue = "pgvector")
public class PgVectorRagStoreService implements RagStore {
    private final VectorStore vectorStore;
    private final MaterialChunkMapper chunkMapper;

    public PgVectorRagStoreService(VectorStore vectorStore, MaterialChunkMapper chunkMapper) {
        this.vectorStore = vectorStore;
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

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("materialId", materialId);
            metadata.put("chunkIndex", i + 1);
            vectorStore.add(List.of(new Document(chunks.get(i), metadata)));
        }
    }

    @Override
    public List<RagSearchResult> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.6)
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(document -> new RagSearchResult(
                        asLong(document.getMetadata().get("materialId")),
                        asInteger(document.getMetadata().get("chunkIndex")),
                        document.getText(),
                        document.getScore()
                ))
                .toList();
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.valueOf(String.valueOf(value));
    }
}
