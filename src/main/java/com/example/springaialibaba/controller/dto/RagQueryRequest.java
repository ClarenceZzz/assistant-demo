package com.example.springaialibaba.controller.dto;

/**
 * Request body for the RAG query endpoint. Carries the raw user question and
 * optional persona/channel hints provided by the caller.
 */
public class RagQueryRequest {

    private String question;

    private String persona;

    private String channel;

    public RagQueryRequest() {
    }

    public RagQueryRequest(String question, String persona, String channel) {
        this.question = question;
        this.persona = persona;
        this.channel = channel;
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
}
