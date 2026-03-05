package com.example.springaialibaba.controller.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Response payload produced by the RAG API.
 */
public class RagQueryResponse {

    private String answer;

    private List<ReferenceDto> references;

    private Double confidence;

    private Long sessionId;

    public RagQueryResponse() {
        this.references = new ArrayList<>();
    }

    public RagQueryResponse(String answer, List<ReferenceDto> references, Double confidence) {
        this.answer = answer;
        setReferences(references);
        this.confidence = confidence;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<ReferenceDto> getReferences() {
        if (references == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(references);
    }

    public void setReferences(List<ReferenceDto> references) {
        if (references == null) {
            this.references = new ArrayList<>();
            return;
        }
        this.references = new ArrayList<>(references);
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RagQueryResponse)) {
            return false;
        }
        RagQueryResponse that = (RagQueryResponse) o;
        return Objects.equals(answer, that.answer)
                && Objects.equals(getReferences(), that.getReferences())
                && Objects.equals(confidence, that.confidence)
                && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer, getReferences(), confidence, sessionId);
    }
}
