package com.example.springaialibaba.chat.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Provides business operations for managing chat sessions and messages.
 */
@Service
public class ChatHistoryService {

    private final ChatSessionRepository chatSessionRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final ObjectMapper objectMapper;

    public ChatHistoryService(ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository, ObjectMapper objectMapper) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    public List<ChatSession> findSessionsByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ChatMessage> findMessagesBySessionId(Long sessionId) {
        ChatSession session = findSessionOrThrow(sessionId);
        return chatMessageRepository.findBySessionIdOrderByCreatedAtDesc(session.id());
    }

    public ChatSession updateSession(Long sessionId, String title, String category) {
        ChatSession existing = findSessionOrThrow(sessionId);
        String updatedTitle = StringUtils.hasText(title) ? title : existing.sessionTitle();
        String updatedCategory = StringUtils.hasText(category) ? category : existing.sessionCategory();
        if (updatedTitle.equals(existing.sessionTitle()) && updatedCategory.equals(existing.sessionCategory())) {
            return existing;
        }
        ChatSession updatedSession = new ChatSession(
                existing.id(),
                existing.userId(),
                updatedTitle,
                updatedCategory,
                existing.sessionStatus(),
                existing.createdAt(),
                existing.updatedAt());
        return chatSessionRepository.save(updatedSession);
    }

    public void deleteSession(Long sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        chatSessionRepository.deleteById(sessionId);
    }

    public ChatMessage saveNewMessage(Long sessionId, String role, String content, String retrievalContext) {
        ChatSession session = findSessionOrThrow(sessionId);
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("消息角色不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        ChatMessageRole messageRole;
        try {
            messageRole = ChatMessageRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的消息角色: " + role, ex);
        }
        JsonNode retrievalContextNode = parseRetrievalContext(retrievalContext);
        ChatMessage message = new ChatMessage(
                null,
                session.id(),
                messageRole,
                content,
                retrievalContextNode,
                null);
        return chatMessageRepository.save(message);
    }

    public ChatSession createOrGetSession(Optional<Long> sessionId, String title, String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (sessionId.isPresent()) {
            return findSessionOrThrow(sessionId.get());
        }
        ChatSession newSession = new ChatSession(
                null,
                userId,
                null,
                null,
                ChatSessionStatus.ACTIVE,
                null,
                null);
        return chatSessionRepository.save(newSession);
    }

    private ChatSession findSessionOrThrow(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));
    }

    private JsonNode parseRetrievalContext(String retrievalContext) {
        if (!StringUtils.hasText(retrievalContext)) {
            return null;
        }
        try {
            return objectMapper.readTree(retrievalContext);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("检索上下文不是有效的 JSON", ex);
        }
    }
}
