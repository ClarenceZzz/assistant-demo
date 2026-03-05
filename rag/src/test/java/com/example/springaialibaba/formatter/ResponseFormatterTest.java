package com.example.springaialibaba.formatter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import com.example.springaialibaba.controller.dto.RagQueryResponse;
import com.example.springaialibaba.controller.dto.ReferenceDto;

class ResponseFormatterTest {

    private DefaultResponseFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new DefaultResponseFormatter();
    }

    @Test
    @DisplayName("完整格式化回答、引用和置信度")
    void testFullResponseFormatting() {
        Document first = new Document("First chunk", Map.of(
                "title", "User Manual",
                "section", "Charging",
                "document_id", "doc-001",
                "chunk_id", "chunk-1"));
        Document second = new Document("Second chunk", Map.of(
                "title", "User Manual",
                "section", "Maintenance",
                "document_id", "doc-001",
                "chunk_id", "chunk-2"));

        RagQueryResponse response = formatter.format("Use the standard charger.", List.of(first, second), 0.9d);

        assertThat(response.getAnswer()).isEqualTo("Use the standard charger.");
        assertThat(response.getConfidence()).isEqualTo(0.81d);
        assertThat(response.getReferences())
                .containsExactly(
                        new ReferenceDto("User Manual", "Charging", "doc-001", "chunk-1"),
                        new ReferenceDto("User Manual", "Maintenance", "doc-001", "chunk-2"));
    }

    @Test
    @DisplayName("元数据字段支持多种命名方式")
    void testMetadataExtractionForReferences() {
        Document document = new Document("Chunk", Map.of(
                "document_title", "Quick Start",
                "section_title", "Install",
                "doc_id", "doc-xyz",
                "chunkId", "chunk-42"));

        RagQueryResponse response = formatter.format("answer", List.of(document), 0.5d);

        assertThat(response.getReferences())
                .containsExactly(new ReferenceDto("Quick Start", "Install", "doc-xyz", "chunk-42"));
    }

    @Test
    @DisplayName("置信度会对输入分数进行截断和非线性映射")
    void testConfidenceCalculation() {
        RagQueryResponse highScore = formatter.format("answer", List.of(new Document("text")), 2.5d);
        RagQueryResponse lowScore = formatter.format("answer", List.of(new Document("text")), -0.3d);
        RagQueryResponse midScore = formatter.format("answer", List.of(new Document("text")), 0.6d);

        assertThat(highScore.getConfidence()).isEqualTo(1.0d);
        assertThat(lowScore.getConfidence()).isEqualTo(0.0d);
        assertThat(midScore.getConfidence()).isEqualTo(0.36d);
    }

    @Test
    @DisplayName("空上下文会返回空引用列表且置信度为 0")
    void testFormattingForFallbackCase() {
        RagQueryResponse response = formatter.format("Fallback", Collections.emptyList(), null);

        assertThat(response.getReferences()).isEmpty();
        assertThat(response.getConfidence()).isZero();
    }
}
