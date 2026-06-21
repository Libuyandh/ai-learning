package com.ailearning.service;

import org.springframework.stereotype.Component;
/*
清洗规则：全文先统一换行：\r\n、\r 转为 \n。
按 Markdown fenced code block 分段，识别行首可带空白的三个及以上连续反引号或波浪线，开闭符号类型一致。
代码块内文本不移除控制字符、不压缩空白、不压缩空行；只继承换行统一。
代码块外移除 \x00-\x08、\x0B、\x0C、\x0E-\x1F。
代码块外逐行将连续空格或制表符压缩为单个空格。
代码块外连续三个及以上空行压缩为两个空行。
 */
@Component
public class TextCleaner {
    public String clean(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder cleaned = new StringBuilder(normalized.length());
        boolean inCodeBlock = false;
        char fenceChar = 0;
        int blankLines = 0;
        int start = 0;
        while (start <= normalized.length()) {
            int end = normalized.indexOf('\n', start);
            boolean hasNewline = end >= 0;
            String line = hasNewline ? normalized.substring(start, end) : normalized.substring(start);
            Fence fence = fence(line);
            if (fence != null && (!inCodeBlock || fence.character == fenceChar)) {
                inCodeBlock = !inCodeBlock;
                fenceChar = inCodeBlock ? fence.character : 0;
                blankLines = 0;
                append(cleaned, line, hasNewline);
            } else if (inCodeBlock) {
                append(cleaned, line, hasNewline);
            } else {
                String cleanedLine = line.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").replaceAll("[ \\t]+", " ");
                if (cleanedLine.trim().isEmpty()) {
                    cleanedLine = "";
                    blankLines++;
                    if (blankLines <= 2) {
                        append(cleaned, cleanedLine, hasNewline);
                    }
                } else {
                    blankLines = 0;
                    append(cleaned, cleanedLine, hasNewline);
                }
            }
            if (!hasNewline) {
                break;
            }
            start = end + 1;
        }
        return cleaned.toString();
    }

    private void append(StringBuilder builder, String line, boolean hasNewline) {
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
