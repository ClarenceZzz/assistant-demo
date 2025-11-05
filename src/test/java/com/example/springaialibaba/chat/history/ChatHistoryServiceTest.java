package com.example.springaialibaba.chat.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    private static final Long SESSION_ID = 42L;

    private static final String USER_ID = "user-1";

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private ChatHistoryService chatHistoryService;

    @BeforeEach
    void setUp() {
        chatHistoryService = new ChatHistoryService(chatSessionRepository, chatMessageRepository, new ObjectMapper());
    }

    @Test
    void shouldFindSessionsByUserId() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, "title", "category", ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(session));

        List<ChatSession> sessions = chatHistoryService.findSessionsByUserId(USER_ID);

        assertThat(sessions).containsExactly(session);
    }

    @Test
    void shouldThrowWhenUserIdBlank() {
        assertThatThrownBy(() -> chatHistoryService.findSessionsByUserId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户ID不能为空");
    }

    @Test
    void shouldFindMessagesBySessionId() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        ChatMessage message = new ChatMessage(1L, SESSION_ID, ChatMessageRole.USER, "hi", null, null);
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of(message));

        List<ChatMessage> messages = chatHistoryService.findMessagesBySessionId(SESSION_ID);

        assertThat(messages).containsExactly(message);
    }

    @Test
    void shouldUpdateSession() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, "old", "default", ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        ChatSession updated = new ChatSession(SESSION_ID, USER_ID, "new", "support", ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(updated);

        ChatSession result = chatHistoryService.updateSession(SESSION_ID, "new", "support");

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void shouldNotUpdateWhenNoChanges() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, "same", "same", ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        ChatSession result = chatHistoryService.updateSession(SESSION_ID, null, " ");

        assertThat(result).isEqualTo(session);
        verify(chatSessionRepository, never()).save(any(ChatSession.class));
    }

    @Test
    void shouldDeleteSession() {
        when(chatSessionRepository.existsById(SESSION_ID)).thenReturn(true);

        chatHistoryService.deleteSession(SESSION_ID);

        verify(chatSessionRepository).deleteById(SESSION_ID);
    }

    @Test
    void shouldThrowWhenDeletingMissingSession() {
        when(chatSessionRepository.existsById(SESSION_ID)).thenReturn(false);

        assertThatThrownBy(() -> chatHistoryService.deleteSession(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("会话不存在");
    }

    @Test
    void shouldSaveNewMessage() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        ChatMessage savedMessage = new ChatMessage(5L, SESSION_ID, ChatMessageRole.USER, "hello", null, null);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessage result = chatHistoryService.saveNewMessage(SESSION_ID, "user", "hello", null);

        assertThat(result).isEqualTo(savedMessage);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage captured = messageCaptor.getValue();
        assertThat(captured.role()).isEqualTo(ChatMessageRole.USER);
        assertThat(captured.sessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void shouldParseRetrievalContext() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatHistoryService.saveNewMessage(SESSION_ID, "assistant", "answer", "{\"foo\":1}");

        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage captured = messageCaptor.getValue();
        assertThat(captured.retrievalContext()).isNotNull();
        assertThat(captured.retrievalContext().get("foo").asInt()).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenRoleInvalid() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> chatHistoryService.saveNewMessage(SESSION_ID, "unknown", "hi", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的消息角色");
    }

    @Test
    void shouldCreateNewSessionWhenMissing() {
        ChatSession saved = new ChatSession(100L, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(saved);

        ChatSession result = chatHistoryService.createOrGetSession(Optional.empty(), USER_ID);

        assertThat(result).isEqualTo(saved);
    }

    @Test
    void shouldReturnExistingSessionWhenIdProvided() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        ChatSession result = chatHistoryService.createOrGetSession(Optional.of(SESSION_ID), USER_ID);

        assertThat(result).isEqualTo(session);
    }

    @Test
    void shouldValidateSessionOwnershipOnFetch() {
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatHistoryService.findMessagesBySessionId(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("会话不存在");
    }

    @Test
    void shouldThrowWhenRetrievalContextInvalid() {
        ChatSession session = new ChatSession(SESSION_ID, USER_ID, null, null, ChatSessionStatus.ACTIVE, null, null);
        when(chatSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> chatHistoryService.saveNewMessage(SESSION_ID, "user", "hi", "not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("检索上下文不是有效的 JSON");
    }

    @Test
    void shouldThrowWhenCreateOrGetWithBlankUserId() {
        assertThatThrownBy(() -> chatHistoryService.createOrGetSession(Optional.empty(), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户ID不能为空");
    }
}
