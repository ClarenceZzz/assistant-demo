package com.example.springaialibaba.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springaialibaba.controller.dto.RagQueryRequest;
import com.example.springaialibaba.controller.dto.RagQueryResponse;
import com.example.springaialibaba.formatter.ResponseFormatter;
import com.example.springaialibaba.generation.GenerationService;
import com.example.springaialibaba.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.retrieval.RetrievalService;

@WebMvcTest(controllers = RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueryPreprocessor queryPreprocessor;

    @MockBean
    private RetrievalService retrievalService;

    @MockBean
    private GenerationService generationService;

    @MockBean
    private ResponseFormatter responseFormatter;

    @Test
    @DisplayName("成功编排完整的 RAG 流程")
    void testSuccessfulRagFlow() throws Exception {
        RagQueryRequest request = new RagQueryRequest("How to charge the EV?", "expert", "web");

        List<Document> documents = List.of(new Document("doc-content", Map.of("score", 0.85)));
        when(queryPreprocessor.process("How to charge the EV?")).thenReturn("how to charge the ev?");
        when(retrievalService.retrieveAndRerank("how to charge the ev?")).thenReturn(documents);
        when(generationService.generate("How to charge the EV?", documents, "expert", "web"))
                .thenReturn("Use the official charger.");
        RagQueryResponse formatted = new RagQueryResponse("Use the official charger.", Collections.emptyList(), 0.85);
        when(responseFormatter.format("Use the official charger.", documents, 0.85)).thenReturn(formatted);

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use the official charger."))
                .andExpect(jsonPath("$.confidence").value(0.85));

        verify(queryPreprocessor).process("How to charge the EV?");
        verify(retrievalService).retrieveAndRerank("how to charge the ev?");
        verify(generationService).generate("How to charge the EV?", documents, "expert", "web");
        verify(responseFormatter).format("Use the official charger.", documents, 0.85);
        verifyNoMoreInteractions(queryPreprocessor, retrievalService, generationService, responseFormatter);
    }

    @Test
    @DisplayName("原始查询会传递到 QueryPreprocessor 并用于检索")
    void testQueryPreprocessingIsCalled() throws Exception {
        RagQueryRequest request = new RagQueryRequest("  raw query  ", null, null);

        List<Document> documents = List.of(new Document("doc"));
        when(queryPreprocessor.process("  raw query  ")).thenReturn("processed");
        when(retrievalService.retrieveAndRerank("processed")).thenReturn(documents);
        when(generationService.generate(eq("  raw query  "), eq(documents), eq("default"), eq("generic")))
                .thenReturn("answer");
        when(responseFormatter.format("answer", documents, null))
                .thenReturn(new RagQueryResponse("answer", Collections.emptyList(), 0.0));

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryPreprocessor).process(queryCaptor.capture());
        verify(retrievalService).retrieveAndRerank("processed");
        verify(generationService).generate("  raw query  ", documents, "default", "generic");
        verify(responseFormatter).format("answer", documents, null);
    }

    @Test
    @DisplayName("检索为空时仍然返回回退回答")
    void testFallbackFlowAtApiLevel() throws Exception {
        RagQueryRequest request = new RagQueryRequest("No docs?", "guest", "app");

        when(queryPreprocessor.process("No docs?")).thenReturn("no docs?");
        when(retrievalService.retrieveAndRerank("no docs?")).thenReturn(Collections.emptyList());
        when(generationService.generate("No docs?", Collections.emptyList(), "guest", "app"))
                .thenReturn("Fallback answer");
        when(responseFormatter.format("Fallback answer", Collections.emptyList(), null))
                .thenReturn(new RagQueryResponse("Fallback answer", Collections.emptyList(), 0.0));

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Fallback answer"))
                .andExpect(jsonPath("$.references").isArray());
    }

    @Test
    @DisplayName("下游异常会被 GlobalExceptionHandler 捕获")
    void testGlobalExceptionHandler() throws Exception {
        RagQueryRequest request = new RagQueryRequest("trigger error", null, null);

        when(queryPreprocessor.process("trigger error")).thenReturn("trigger error");
        when(retrievalService.retrieveAndRerank("trigger error"))
                .thenThrow(new RuntimeException("retrieval failed"));

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("retrieval failed"));
    }

    @Test
    @DisplayName("缺少问题时返回 400")
    void testBlankQuestionIsRejected() throws Exception {
        RagQueryRequest request = new RagQueryRequest("   ", null, null);

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
