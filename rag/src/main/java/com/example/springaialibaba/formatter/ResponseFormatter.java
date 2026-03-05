package com.example.springaialibaba.formatter;

import java.util.List;

import org.springframework.ai.document.Document;

import com.example.springaialibaba.controller.dto.RagQueryResponse;

/**
 * Abstraction responsible for converting the raw RAG outputs into a
 * client-facing response structure.
 */
public interface ResponseFormatter {

    /**
     * Format the RAG result into a {@link RagQueryResponse}.
     *
     * @param answer the generated answer text
     * @param context the retrieved supporting documents
     * @param topRerankScore the highest rerank score, may be {@code null}
     * @return formatted response
     */
    RagQueryResponse format(String answer, List<Document> context, Double topRerankScore);
}
