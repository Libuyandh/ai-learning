package com.ailearning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtils() {
    }

    static long longAt(String json, String pointer) throws Exception {
        return nodeAt(json, pointer).asLong();
    }

    static String textAt(String json, String pointer) throws Exception {
        return nodeAt(json, pointer).asText();
    }

    private static JsonNode nodeAt(String json, String pointer) throws Exception {
        return OBJECT_MAPPER.readTree(json).at(pointer);
    }
}
