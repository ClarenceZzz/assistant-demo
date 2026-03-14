package com.example.springaialibaba.model.dto;

import java.util.Map;

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

    private String documentSource;

    private String documentType;

    private String dateFrom;

    private String dateTo;

    private Map<String, String> filters;

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

    public String getDocumentSource() {
        return documentSource;
    }

    public void setDocumentSource(String documentSource) {
        this.documentSource = documentSource;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }
}
