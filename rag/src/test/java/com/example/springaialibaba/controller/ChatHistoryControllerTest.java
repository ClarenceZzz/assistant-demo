package com.example.springaialibaba.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springaialibaba.chat.history.ChatHistoryService;
import com.example.springaialibaba.chat.history.ChatMessage;
import com.example.springaialibaba.chat.history.ChatMessageRole;
import com.example.springaialibaba.chat.history.ChatSession;
import com.example.springaialibaba.chat.history.ChatSessionStatus;
import com.example.springaialibaba.controller.dto.UpdateChatSessionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ChatHistoryController.class)
class ChatHistoryControllerTest {

    private static final String BASE_URL = "/api/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatHistoryService chatHistoryService;

    @Test
    @DisplayName("返回指定用户的聊天会话列表")
    void testGetSessions_Success() throws Exception {
        ChatSession session = new ChatSession(1L, "user-1", "欢迎", "default", ChatSessionStatus.ACTIVE,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(chatHistoryService.findSessionsByUserId("user-1")).thenReturn(List.of(session));

        mockMvc.perform(get(BASE_URL + "/sessions").param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].sessionTitle").value("欢迎"));

        verify(chatHistoryService).findSessionsByUserId("user-1");
        verifyNoMoreInteractions(chatHistoryService);
    }

    @Test
    @DisplayName("返回指定会话的消息列表")
    void testGetMessages_Success() throws Exception {
        ChatMessage message = new ChatMessage(5L, 9L, ChatMessageRole.USER, "你好", null, OffsetDateTime.now());
        when(chatHistoryService.findMessagesBySessionId(9L)).thenReturn(List.of(message));

        mockMvc.perform(get(BASE_URL + "/messages/{sessionId}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value("你好"));

        verify(chatHistoryService).findMessagesBySessionId(9L);
        verifyNoMoreInteractions(chatHistoryService);
    }

    @Test
    @DisplayName("成功更新聊天会话信息")
    void testUpdateSession_Success() throws Exception {
        UpdateChatSessionRequest request = new UpdateChatSessionRequest("新的标题", "support");
        ChatSession updated = new ChatSession(8L, "user-8", "新的标题", "support", ChatSessionStatus.ACTIVE,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(chatHistoryService.updateSession(eq(8L), eq("新的标题"), eq("support"))).thenReturn(updated);

        mockMvc.perform(put(BASE_URL + "/sessions/{sessionId}", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionTitle").value("新的标题"))
                .andExpect(jsonPath("$.sessionCategory").value("support"));

        verify(chatHistoryService).updateSession(8L, "新的标题", "support");
        verifyNoMoreInteractions(chatHistoryService);
    }

    @Test
    @DisplayName("删除会话返回 204 No Content")
    void testDeleteSession_Success() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/sessions/{sessionId}", 11L))
                .andExpect(status().isNoContent());

        verify(chatHistoryService).deleteSession(11L);
        verifyNoMoreInteractions(chatHistoryService);
    }

    @Test
    @DisplayName("查询不存在的会话消息返回 404")
    void testGetMessages_NotFound() throws Exception {
        when(chatHistoryService.findMessagesBySessionId(404L))
                .thenThrow(new IllegalArgumentException("会话不存在: 404"));

        mockMvc.perform(get(BASE_URL + "/messages/{sessionId}", 404L))
                .andExpect(status().isNotFound());

        verify(chatHistoryService).findMessagesBySessionId(404L);
        verifyNoMoreInteractions(chatHistoryService);
    }
}
