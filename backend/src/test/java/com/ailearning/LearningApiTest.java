package com.ailearning;

import com.ailearning.search.MockWebSearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearningApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MockWebSearchClient webSearchClient;

    @Test
    void createSessionRejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/learning/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"text\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void completesLearningFlow() throws Exception {
        String createBody = "{\"inputType\":\"text\",\"content\":\"光合作用是绿色植物利用光能，将二氧化碳和水转化为有机物，并释放氧气的过程。\"}";

        String createResponse = mockMvc.perform(post("/api/learning/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isNumber())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();

        long sessionId = JsonTestUtils.longAt(createResponse, "/data/sessionId");

        String questionsResponse = mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.questions", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.questions[0].options", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.questions[0].correctAnswer").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        long firstQuestionId = JsonTestUtils.longAt(questionsResponse, "/data/questions/0/id");
        String firstAnswer = JsonTestUtils.textAt(questionsResponse, "/data/questions/0/correctAnswer");
        long secondQuestionId = JsonTestUtils.longAt(questionsResponse, "/data/questions/1/id");

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + firstQuestionId + ",\"userAnswer\":\"" + firstAnswer + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.correctAnswer").value(firstAnswer))
                .andExpect(jsonPath("$.data.progress.answeredCount").value(1));

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + secondQuestionId + ",\"userAnswer\":\"错误答案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.explanation").isNotEmpty());

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + firstQuestionId + ",\"userAnswer\":\"" + firstAnswer + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_EXISTS"));

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").isNumber())
                .andExpect(jsonPath("$.data.score").isNumber())
                .andExpect(jsonPath("$.data.accuracy").isNumber())
                .andExpect(jsonPath("$.data.weakPoints").isArray())
                .andExpect(jsonPath("$.data.suggestions").isArray())
                .andExpect(jsonPath("$.data.wrongQuestions").isArray());

        mockMvc.perform(get("/api/learning/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.material.content").isNotEmpty())
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.answers").isArray())
                .andExpect(jsonPath("$.data.report.reportId").isNumber());
    }

    @Test
    void searchesWebForNewKnowledgeAndReusesGeneratedQuestions() throws Exception {
        int callsBefore = webSearchClient.callCount();
        String createResponse = mockMvc.perform(post("/api/learning/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"text\",\"content\":\"Harness Engineering\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long sessionId = JsonTestUtils.longAt(createResponse, "/data/sessionId");

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].sourceUrl").value("https://example.com/search-result"))
                .andExpect(jsonPath("$.data.questions[0].evidence").isNotEmpty())
                .andExpect(jsonPath("$.data.questions[0].confidence").value(0.9));

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/questions"))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(callsBefore + 1, webSearchClient.callCount());
    }

    @Test
    void rejectsQuestionGenerationWhenSearchHasNoResults() throws Exception {
        String createResponse = mockMvc.perform(post("/api/learning/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputType\":\"text\",\"content\":\"__EMPTY__\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long sessionId = JsonTestUtils.longAt(createResponse, "/data/sessionId");

        mockMvc.perform(post("/api/learning/sessions/" + sessionId + "/questions"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_GENERATION_FAILED"));
    }
}
