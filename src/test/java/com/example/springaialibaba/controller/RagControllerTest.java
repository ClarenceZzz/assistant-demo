package com.example.springaialibaba.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.example.springaialibaba.chat.history.ChatHistoryService;
import com.example.springaialibaba.chat.history.ChatSession;
import com.example.springaialibaba.chat.history.ChatSessionStatus;
import com.example.springaialibaba.controller.dto.RagQueryRequest;
import com.example.springaialibaba.controller.dto.RagQueryResponse;
import com.example.springaialibaba.controller.dto.ReferenceDto;
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

    @MockBean
    private ChatHistoryService chatHistoryService;

    @Test
    @DisplayName("成功编排完整的 RAG 流程")
    void testSuccessfulRagFlow() throws Exception {
        RagQueryRequest request = new RagQueryRequest("How to charge the EV?", "expert", "web");
        request.setUserId("test-user");

        List<Document> documents = List.of(new Document("doc-content", Map.of("score", 0.85)));
        when(queryPreprocessor.process("How to charge the EV?")).thenReturn("how to charge the ev?");
        when(retrievalService.retrieveAndRerank("how to charge the ev?")).thenReturn(documents);
        when(generationService.generate("How to charge the EV?", documents, "expert", "web"))
                .thenReturn("Use the official charger.");
        ReferenceDto reference = new ReferenceDto("doc-1", "section-1", "document-123", "chunk-1");
        RagQueryResponse formatted = new RagQueryResponse("Use the official charger.", List.of(reference), 0.85);
        when(responseFormatter.format("Use the official charger.", documents, 0.85)).thenReturn(formatted);
        ChatSession session = new ChatSession(42L, "test-user", null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatHistoryService.createOrGetSession(any(), anyString())).thenReturn(session);

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use the official charger."))
                .andExpect(jsonPath("$.confidence").value(0.85))
                .andExpect(jsonPath("$.sessionId").value(42L));

        verify(queryPreprocessor).process("How to charge the EV?");
        verify(retrievalService).retrieveAndRerank("how to charge the ev?");
        verify(generationService).generate("How to charge the EV?", documents, "expert", "web");
        verify(responseFormatter).format("Use the official charger.", documents, 0.85);
        ArgumentCaptor<Optional<Long>> sessionIdCaptor = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatHistoryService).createOrGetSession(sessionIdCaptor.capture(), userIdCaptor.capture());
        verify(chatHistoryService).saveNewMessage(eq(42L), eq("USER"), eq("How to charge the EV?"), isNull());
        ArgumentCaptor<String> retrievalCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatHistoryService).saveNewMessage(eq(42L), eq("ASSISTANT"), eq("Use the official charger."),
                retrievalCaptor.capture());
        verifyNoMoreInteractions(queryPreprocessor, retrievalService, generationService, responseFormatter);
        verifyNoMoreInteractions(chatHistoryService);

        assertThat(sessionIdCaptor.getValue()).isEmpty();
        assertThat(userIdCaptor.getValue()).isEqualTo("test-user");
        assertThat(retrievalCaptor.getValue()).isNotBlank();
        assertThat(objectMapper.readTree(retrievalCaptor.getValue()).get(0).get("documentId").asText())
                .isEqualTo("document-123");
    }

    @Test
    @DisplayName("原始查询会传递到 QueryPreprocessor 并用于检索")
    void testQueryPreprocessingIsCalled() throws Exception {
        RagQueryRequest request = new RagQueryRequest("  raw query  ", null, null);
        request.setUserId("csr-1");

        List<Document> documents = List.of(new Document("doc"));
        when(queryPreprocessor.process("  raw query  ")).thenReturn("processed");
        when(retrievalService.retrieveAndRerank("processed")).thenReturn(documents);
        when(generationService.generate(eq("  raw query  "), eq(documents), eq("default"), eq("generic")))
                .thenReturn("answer");
        when(responseFormatter.format("answer", documents, null))
                .thenReturn(new RagQueryResponse("answer", Collections.emptyList(), 0.0));
        ChatSession session = new ChatSession(7L, "csr-1", null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatHistoryService.createOrGetSession(any(), anyString())).thenReturn(session);

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryPreprocessor).process(queryCaptor.capture());
        verify(retrievalService).retrieveAndRerank("processed");
        verify(generationService).generate("  raw query  ", documents, "default", "generic");
        verify(responseFormatter).format("answer", documents, null);
        verify(chatHistoryService).createOrGetSession(eq(Optional.empty()), eq("csr-1"));
        verify(chatHistoryService).saveNewMessage(eq(7L), eq("USER"), eq("  raw query  "), isNull());
        verify(chatHistoryService).saveNewMessage(eq(7L), eq("ASSISTANT"), eq("answer"), isNull());
    }

    @Test
    @DisplayName("检索为空时仍然返回回退回答")
    void testFallbackFlowAtApiLevel() throws Exception {
        RagQueryRequest request = new RagQueryRequest("No docs?", "guest", "app");
        request.setUserId("guest-user");

        when(queryPreprocessor.process("No docs?")).thenReturn("no docs?");
        when(retrievalService.retrieveAndRerank("no docs?")).thenReturn(Collections.emptyList());
        when(generationService.generate("No docs?", Collections.emptyList(), "guest", "app"))
                .thenReturn("Fallback answer");
        RagQueryResponse response = new RagQueryResponse("Fallback answer", Collections.emptyList(), 0.0);
        when(responseFormatter.format("Fallback answer", Collections.emptyList(), null))
                .thenReturn(response);
        ChatSession session = new ChatSession(88L, "guest-user", null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatHistoryService.createOrGetSession(any(), anyString())).thenReturn(session);

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Fallback answer"))
                .andExpect(jsonPath("$.references").isArray())
                .andExpect(jsonPath("$.sessionId").value(88L));

        verify(chatHistoryService).createOrGetSession(eq(Optional.empty()), eq("guest-user"));
        verify(chatHistoryService).saveNewMessage(eq(88L), eq("USER"), eq("No docs?"), isNull());
        verify(chatHistoryService).saveNewMessage(eq(88L), eq("ASSISTANT"), eq("Fallback answer"), isNull());
    }

    @Test
    @DisplayName("下游异常会被 GlobalExceptionHandler 捕获")
    void testGlobalExceptionHandler() throws Exception {
        RagQueryRequest request = new RagQueryRequest("trigger error", null, null);
        request.setUserId("error-user");

        when(queryPreprocessor.process("trigger error")).thenReturn("trigger error");
        when(retrievalService.retrieveAndRerank("trigger error"))
                .thenThrow(new RuntimeException("retrieval failed"));

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("retrieval failed"));

        verify(queryPreprocessor).process("trigger error");
        verify(retrievalService).retrieveAndRerank("trigger error");
        verifyNoInteractions(chatHistoryService);
        verifyNoMoreInteractions(queryPreprocessor, retrievalService, generationService, responseFormatter);
    }

    @Test
    @DisplayName("缺少问题时返回 400")
    void testBlankQuestionIsRejected() throws Exception {
        RagQueryRequest request = new RagQueryRequest("   ", null, null);
        request.setUserId("user-123");

        mockMvc.perform(post("/api/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoInteractions(chatHistoryService);
    }
}
