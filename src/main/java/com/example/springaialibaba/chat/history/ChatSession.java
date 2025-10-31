package com.example.springaialibaba.chat.history;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("chat_session")
public record ChatSession(
        @Id Long id,
        @Column("user_id") String userId,
        @Column("session_title") String sessionTitle,
        @Column("session_category") String sessionCategory,
        @Column("session_status") ChatSessionStatus sessionStatus,
        @Column("retrieval_context") JsonNode retrievalContext,
        @ReadOnlyProperty @Column("created_at") OffsetDateTime createdAt,
        @ReadOnlyProperty @Column("updated_at") OffsetDateTime updatedAt) {

    public ChatSession withId(Long id) {
        return new ChatSession(
                id,
                userId,
                sessionTitle,
                sessionCategory,
                sessionStatus,
                retrievalContext,
                createdAt,
                updatedAt);
    }
}
