package com.ailearning.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextCleanerTest {
    private final TextCleaner textCleaner = new TextCleaner();

    @Test
    void normalizesNewlines() {
        assertEquals("a\nb\nc", textCleaner.clean("a\r\nb\rc"));
    }

    @Test
    void removesControlCharactersOutsideCodeBlocks() {
        assertEquals("abc\n", textCleaner.clean("a\u0000b\u0008c\u000B\u000C\u000E\n"));
    }

    @Test
    void compressesSpacesAndTabsOutsideCodeBlocks() {
        assertEquals("a b c\n", textCleaner.clean("a   b\t\tc\n"));
    }

    @Test
    void compressesMoreThanTwoBlankLinesOutsideCodeBlocks() {
        assertEquals("a\n\n\nb", textCleaner.clean("a\n\n\n\nb"));
    }

    @Test
    void preservesBacktickCodeBlocks() {
        String text = "a   b\n```\nx\t\t y\u0000\n\n\nz\n```\nc\t d";
        assertEquals("a b\n```\nx\t\t y\u0000\n\n\nz\n```\nc d", textCleaner.clean(text));
    }

    @Test
    void preservesTildeCodeBlocks() {
        String text = "~~~java\r\nx\t\t y\u0000\r\n\r\n\r\n~~~\r\na\t\tb";
        assertEquals("~~~java\nx\t\t y\u0000\n\n\n~~~\na b", textCleaner.clean(text));
    }
}
