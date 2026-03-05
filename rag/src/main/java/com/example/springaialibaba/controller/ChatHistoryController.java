package com.example.springaialibaba.controller;

import com.example.springaialibaba.chat.history.ChatHistoryService;
import com.example.springaialibaba.chat.history.ChatMessage;
import com.example.springaialibaba.chat.history.ChatSession;
import com.example.springaialibaba.controller.dto.UpdateChatSessionRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD-style endpoints for chat session history.
 */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping(path = "/sessions")
    public ResponseEntity<List<ChatSession>> getSessions(@RequestParam("userId") String userId) {
        List<ChatSession> sessions = chatHistoryService.findSessionsByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping(path = "/messages/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable("sessionId") Long sessionId) {
        try {
            List<ChatMessage> messages = chatHistoryService.findMessagesBySessionId(sessionId);
            return ResponseEntity.ok(messages);
        }
        catch (IllegalArgumentException ex) {
            if (isSessionMissing(ex)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw ex;
        }
    }

    @PutMapping(path = "/sessions/{sessionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatSession> updateSession(@PathVariable("sessionId") Long sessionId,
            @RequestBody UpdateChatSessionRequest request) {
        try {
            ChatSession updated = chatHistoryService.updateSession(sessionId, request.title(), request.category());
            return ResponseEntity.ok(updated);
        }
        catch (IllegalArgumentException ex) {
            if (isSessionMissing(ex)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw ex;
        }
    }

    @DeleteMapping(path = "/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable("sessionId") Long sessionId) {
        try {
            chatHistoryService.deleteSession(sessionId);
            return ResponseEntity.noContent().build();
        }
        catch (IllegalArgumentException ex) {
            if (isSessionMissing(ex)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw ex;
        }
    }

    private boolean isSessionMissing(IllegalArgumentException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("会话不存在");
    }
}
