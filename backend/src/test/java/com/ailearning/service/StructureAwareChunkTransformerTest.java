package com.ailearning.service;

import com.ailearning.config.ChunkingProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureAwareChunkTransformerTest {
    @Test
    void splitsByHeadingsOutsideCodeBlocks() {
        StructureAwareChunkTransformer transformer = transformer(8, 12, 0);
        List<String> chunks = transformer.transform("# A\nalpha text\n```\n# not heading\n```\n# B\nbeta text");

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("# not heading"));
        assertTrue(chunks.get(1).startsWith("# B"));
    }

    @Test
    void splitsOversizedSectionByParagraphsFirst() {
        StructureAwareChunkTransformer transformer = transformer(8, 10, 0);
        List<String> chunks = transformer.transform("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\n\nbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        assertEquals(2, chunks.size());
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", chunks.get(0));
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", chunks.get(1));
    }

    @Test
    void splitsOversizedParagraphBySentences() {
        StructureAwareChunkTransformer transformer = transformer(5, 6, 0);
        List<String> chunks = transformer.transform("第一句很长很长很长。第二句很长很长很长!第三句很长很长很长;");

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).endsWith("。"));
        assertTrue(chunks.get(1).endsWith("!"));
        assertTrue(chunks.get(2).endsWith(";"));
    }

    @Test
    void hardSplitsOversizedTextWithoutSentenceBoundary() {
        StructureAwareChunkTransformer transformer = transformer(10, 10, 0);
        List<String> chunks = transformer.transform("x".repeat(90));

        assertEquals(3, chunks.size());
        assertEquals(40, chunks.get(0).length());
        assertEquals(40, chunks.get(1).length());
        assertEquals(10, chunks.get(2).length());
    }

    @Test
    void greedilyMergesWithoutExceedingMaxTokens() {
        StructureAwareChunkTransformer transformer = transformer(10, 12, 0);
        List<String> chunks = transformer.transform("aaaaaaaaaa\n\nbbbbbbbbbb\n\ncccccccccc\n\ndddddddddd\n\neeeeeeeeee");

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("aaaaaaaaaa"));
        assertTrue(chunks.get(0).contains("bbbbbbbbbb"));
        assertTrue(chunks.get(0).contains("cccccccccc"));
        assertFalse(chunks.get(0).contains("dddddddddd"));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 48));
    }

    @Test
    void addsConfiguredOverlapBetweenChunks() {
        StructureAwareChunkTransformer transformer = transformer(5, 8, 2);
        List<String> chunks = transformer.transform("abcdefghij\n\nklmnopqrst\n\nuvwxyz1234");

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(1).startsWith("cdefghij"));
        assertTrue(chunks.get(2).startsWith("mnopqrst"));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 32));
    }

    private StructureAwareChunkTransformer transformer(int targetTokens, int maxTokens, int overlapTokens) {
        ChunkingProperties properties = new ChunkingProperties();
        properties.setTargetTokens(targetTokens);
        properties.setMaxTokens(maxTokens);
        properties.setOverlapTokens(overlapTokens);
        return new StructureAwareChunkTransformer(properties);
    }
}
