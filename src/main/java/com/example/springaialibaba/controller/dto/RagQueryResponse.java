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
                && Objects.equals(confidence, that.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer, getReferences(), confidence);
    }
}
