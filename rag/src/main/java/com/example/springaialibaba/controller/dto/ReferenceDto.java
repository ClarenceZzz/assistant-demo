package com.example.springaialibaba.controller.dto;

import java.util.Objects;

/**
 * DTO describing a single reference document returned alongside the RAG
 * answer.
 */
public class ReferenceDto {

    private String title;

    private String section;

    private String documentId;

    private String chunkId;

    public ReferenceDto() {
    }

    public ReferenceDto(String title, String section, String documentId, String chunkId) {
        this.title = title;
        this.section = section;
        this.documentId = documentId;
        this.chunkId = chunkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceDto)) {
            return false;
        }
        ReferenceDto that = (ReferenceDto) o;
        return Objects.equals(title, that.title)
                && Objects.equals(section, that.section)
                && Objects.equals(documentId, that.documentId)
                && Objects.equals(chunkId, that.chunkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, section, documentId, chunkId);
    }
}
