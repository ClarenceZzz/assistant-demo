package com.example.springaialibaba.formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.example.springaialibaba.controller.dto.RagQueryResponse;
import com.example.springaialibaba.controller.dto.ReferenceDto;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link ResponseFormatter} that builds the API
 * response by combining the generated answer, reference metadata and rerank
 * scores.
 */
@Service
public class DefaultResponseFormatter implements ResponseFormatter {

    @Override
    public RagQueryResponse format(String answer, List<Document> context, Double topRerankScore) {
        RagQueryResponse response = new RagQueryResponse();
        response.setAnswer(answer);
        response.setReferences(buildReferences(context));
        response.setConfidence(calculateConfidence(topRerankScore));
        return response;
    }

    private List<ReferenceDto> buildReferences(List<Document> context) {
        if (context == null || context.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReferenceDto> references = new ArrayList<>(context.size());
        for (Document document : context) {
            Map<String, Object> metadata = document.getMetadata();
            ReferenceDto reference = new ReferenceDto();
            reference.setTitle(extractString(metadata, "title", "document_title"));
            reference.setSection(extractString(metadata, "section", "section_title"));
            reference.setDocumentId(extractString(metadata, "document_id", "doc_id", "documentId"));
            if (!StringUtils.hasText(reference.getDocumentId())) {
                reference.setDocumentId(extractString(metadata, "title"));
            }
            reference.setChunkId(extractString(metadata, "chunk_id", "chunkId", "id"));
            if (!StringUtils.hasText(reference.getChunkId())) {
                reference.setChunkId(extractString(metadata, "title") + "-" + extractString(metadata, "chunk_index"));
            }
            references.add(reference);
        }
        return references;
    }

    private String extractString(Map<String, Object> metadata, String... keys) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            if (!metadata.containsKey(key)) {
                continue;
            }
            Object value = metadata.get(key);
            if (value == null) {
                continue;
            }
            String asString = Objects.toString(value, null);
            if (asString != null && !asString.isBlank()) {
                return asString;
            }
        }
        return null;
    }

    private double calculateConfidence(Double topRerankScore) {
        if (topRerankScore == null) {
            return 0.0d;
        }
        double clamped = Math.max(0.0d, Math.min(1.0d, topRerankScore));
        return Math.pow(clamped, 2.0d);
    }
}
