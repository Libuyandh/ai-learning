package com.ailearning.service;

import com.ailearning.config.ChunkingProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StructureAwareChunkTransformer {
    static final int CHARS_PER_TOKEN = 4;
    private final ChunkingProperties properties;

    public StructureAwareChunkTransformer(ChunkingProperties properties) {
        this.properties = properties;
    }

    public List<String> transform(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> pieces = new ArrayList<>();
        for (String section : splitSections(text.trim())) {
            pieces.addAll(splitOversized(section));
        }
        return merge(pieces);
    }

    private List<String> splitSections(String text) {
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inCodeBlock = false;
        char fenceChar = 0;
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf('\n', start);
            boolean hasNewline = end >= 0;
            String line = hasNewline ? text.substring(start, end) : text.substring(start);
            Fence fence = fence(line);
            if (fence != null && (!inCodeBlock || fence.character == fenceChar)) {
                inCodeBlock = !inCodeBlock;
                fenceChar = inCodeBlock ? fence.character : 0;
            }
            if (!inCodeBlock && isHeading(line) && !current.isEmpty()) {
                addIfText(sections, current.toString());
                current.setLength(0);
            }
            appendLine(current, line, hasNewline);
            if (!hasNewline) {
                break;
            }
            start = end + 1;
        }
        addIfText(sections, current.toString());
        return sections;
    }

    private List<String> splitOversized(String text) {
        String item = text.trim();
        if (tokens(item) <= properties.getMaxTokens()) {
            return List.of(item);
        }
        List<String> pieces = new ArrayList<>();
        for (String paragraph : item.split("\\n\\s*\\n+")) {
            String paragraphText = paragraph.trim();
            if (!StringUtils.hasText(paragraphText)) {
                continue;
            }
            if (tokens(paragraphText) <= properties.getMaxTokens()) {
                pieces.add(paragraphText);
            } else {
                pieces.addAll(splitSentences(paragraphText));
            }
        }
        return pieces;
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            current.append(character);
            if (isSentenceEnd(character)) {
                addSentence(sentences, current);
            }
        }
        addSentence(sentences, current);
        List<String> pieces = new ArrayList<>();
        for (String sentence : sentences) {
            if (tokens(sentence) <= properties.getMaxTokens()) {
                pieces.add(sentence);
            } else {
                pieces.addAll(hardSplit(sentence));
            }
        }
        return pieces;
    }

    private List<String> hardSplit(String text) {
        List<String> pieces = new ArrayList<>();
        int size = properties.getMaxTokens() * CHARS_PER_TOKEN;
        for (int start = 0; start < text.length(); start += size) {
            addIfText(pieces, text.substring(start, Math.min(start + size, text.length())));
        }
        return pieces;
    }

    private List<String> merge(List<String> pieces) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            String item = piece.trim();
            if (!StringUtils.hasText(item)) {
                continue;
            }
            if (current.isEmpty()) {
                current.append(item);
            } else {
                String candidate = current + "\n\n" + item;
                if (tokens(candidate) <= properties.getTargetTokens()) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    chunks.add(current.toString());
                    current.setLength(0);
                    current.append(item);
                }
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return addOverlap(chunks);
    }

    private List<String> addOverlap(List<String> chunks) {
        if (chunks.size() <= 1 || properties.getOverlapTokens() <= 0) {
            return chunks;
        }
        List<String> overlapped = new ArrayList<>();
        int overlapChars = properties.getOverlapTokens() * CHARS_PER_TOKEN;
        int maxChars = properties.getMaxTokens() * CHARS_PER_TOKEN;
        overlapped.add(chunks.get(0));
        for (int i = 1; i < chunks.size(); i++) {
            String previous = chunks.get(i - 1);
            String overlap = previous.substring(Math.max(0, previous.length() - overlapChars));
            String chunk = overlap + chunks.get(i);
            if (chunk.length() > maxChars) {
                chunk = chunk.substring(0, maxChars);
            }
            overlapped.add(chunk);
        }
        return overlapped;
    }

    private int tokens(String text) {
        return (int) Math.ceil((double) text.length() / CHARS_PER_TOKEN);
    }

    private boolean isHeading(String line) {
        int index = 0;
        while (index < line.length() && line.charAt(index) == ' ') {
            index++;
        }
        int count = 0;
        while (index < line.length() && line.charAt(index) == '#') {
            count++;
            index++;
        }
        return count >= 1 && count <= 6 && index < line.length() && Character.isWhitespace(line.charAt(index));
    }

    private boolean isSentenceEnd(char character) {
        return "。！？；!?;".indexOf(character) >= 0;
    }

    private void addSentence(List<String> sentences, StringBuilder current) {
        addIfText(sentences, current.toString());
        current.setLength(0);
    }

    private void addIfText(List<String> items, String text) {
        String item = text.trim();
        if (StringUtils.hasText(item)) {
            items.add(item);
        }
    }

    private void appendLine(StringBuilder builder, String line, boolean hasNewline) {
        builder.append(line);
        if (hasNewline) {
            builder.append('\n');
        }
    }

    private Fence fence(String line) {
        int index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        if (index >= line.length()) {
            return null;
        }
        char character = line.charAt(index);
        if (character != '`' && character != '~') {
            return null;
        }
        int count = 0;
        while (index < line.length() && line.charAt(index) == character) {
            count++;
            index++;
        }
        return count >= 3 ? new Fence(character) : null;
    }

    private record Fence(char character) {
    }
}
