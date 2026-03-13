package com.example.springaialibaba.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springaialibaba.core.formatter.ResponseFormatter;
import com.example.springaialibaba.model.dto.RagQueryRequest;
import com.example.springaialibaba.model.dto.RagQueryResponse;
import com.example.springaialibaba.model.dto.ReferenceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ModularRagController.class)
class ModularRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "ragChatClient")
    private ChatClient ragChatClient;

    @MockBean
    private ResponseFormatter responseFormatter;

    @Test
    @DisplayName("成功走 Advisor 链路并保持响应兼容")
    void testSuccessfulAdvisorFlow() throws Exception {
        RagQueryRequest request = new RagQueryRequest("How to charge the EV?", "expert", "web");
        List<Document> documents = List.of(new Document("doc-content", Map.of("score", 0.85)));
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClientResponse advisorResponse = buildAdvisorResponse("Use the official charger.", documents);

        when(ragChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatClientResponse()).thenReturn(advisorResponse);

        ReferenceDto reference = new ReferenceDto("doc-1", "section-1", "document-123", "chunk-1");
        RagQueryResponse formatted = new RagQueryResponse("Use the official charger.", List.of(reference), 0.85);
        when(responseFormatter.format("Use the official charger.", documents, 0.85)).thenReturn(formatted);

        mockMvc.perform(post("/api/v1/rag/modular/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use the official charger."))
                .andExpect(jsonPath("$.confidence").value(0.85));

        verify(requestSpec).user("How to charge the EV?");
        verify(responseFormatter).format("Use the official charger.", documents, 0.85);

        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec).advisors(advisorCaptor.capture());
        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
        when(advisorSpec.param(anyString(), any())).thenReturn(advisorSpec);
        advisorCaptor.getValue().accept(advisorSpec);
        verify(advisorSpec).param("originalQuestion", "How to charge the EV?");
        verify(advisorSpec).param("persona", "expert");
        verify(advisorSpec).param("channel", "web");
    }

    @Test
    @DisplayName("缺省 persona/channel 会透传默认值")
    void testDefaultPersonaAndChannel() throws Exception {
        RagQueryRequest request = new RagQueryRequest("  raw query  ", null, null);
        List<Document> documents = List.of(new Document("doc"));
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClientResponse advisorResponse = buildAdvisorResponse("answer", documents);

        when(ragChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatClientResponse()).thenReturn(advisorResponse);
        when(responseFormatter.format("answer", documents, null))
                .thenReturn(new RagQueryResponse("answer", Collections.emptyList(), 0.0));

        mockMvc.perform(post("/api/v1/rag/modular/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        verify(responseFormatter).format("answer", documents, null);
        ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec).advisors(advisorCaptor.capture());
        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
        when(advisorSpec.param(anyString(), any())).thenReturn(advisorSpec);
        advisorCaptor.getValue().accept(advisorSpec);
        verify(advisorSpec).param("persona", "客服人员");
        verify(advisorSpec).param("channel", "售后服务");
    }

    @Test
    @DisplayName("检索为空时仍返回兼容结构")
    void testFallbackFlowAtApiLevel() throws Exception {
        RagQueryRequest request = new RagQueryRequest("No docs?", "guest", "app");
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ChatClientResponse advisorResponse = buildAdvisorResponse("Fallback answer", Collections.emptyList());

        when(ragChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatClientResponse()).thenReturn(advisorResponse);
        when(responseFormatter.format("Fallback answer", Collections.emptyList(), null))
                .thenReturn(new RagQueryResponse("Fallback answer", Collections.emptyList(), 0.0));

        mockMvc.perform(post("/api/v1/rag/modular/query")
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
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(ragChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("advisor failed"));

        mockMvc.perform(post("/api/v1/rag/modular/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("advisor failed"));

        verifyNoInteractions(responseFormatter);
    }

    @Test
    @DisplayName("缺少问题时返回 400")
    void testBlankQuestionIsRejected() throws Exception {
        RagQueryRequest request = new RagQueryRequest("   ", null, null);

        mockMvc.perform(post("/api/v1/rag/modular/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoInteractions(ragChatClient, responseFormatter);
    }

    private ChatClientResponse buildAdvisorResponse(String answer, List<Document> documents) {
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        return new ChatClientResponse(chatResponse,
                Map.of(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, documents));
    }
}
