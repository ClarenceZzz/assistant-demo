package com.example.springaialibaba.controller.dto;

/**
 * Request body for the RAG query endpoint. Carries the raw user question and
 * optional persona/channel hints provided by the caller.
 */
public class RagQueryRequest {

    private String question;

    private String persona;

    private String channel;

    private Long sessionId;

    private String userId;

    public RagQueryRequest() {
    }

    public RagQueryRequest(String question, String persona, String channel) {
        this(question, persona, channel, null, null);
    }

    public RagQueryRequest(String question, String persona, String channel, Long sessionId, String userId) {
        this.question = question;
        this.persona = persona;
        this.channel = channel;
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
