package com.example.springaialibaba.chat.history;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("chat_message")
public record ChatMessage(
        @Id Long id,
        @Column("session_id") Long sessionId,
        @Column("role") ChatMessageRole role,
        @Column("content") String content,
        @Column("retrieval_context") JsonNode retrievalContext,
        @ReadOnlyProperty @Column("created_at") OffsetDateTime createdAt) {

    public ChatMessage withId(Long id) {
        return new ChatMessage(id, sessionId, role, content, retrievalContext, createdAt);
    }
}
