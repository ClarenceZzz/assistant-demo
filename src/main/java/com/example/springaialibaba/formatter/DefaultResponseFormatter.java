package com.example.springaialibaba.formatter;

import java.util.Collections;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.example.springaialibaba.controller.dto.RagQueryResponse;

/**
 * Temporary, minimal implementation of {@link ResponseFormatter}. The detailed
 * formatting logic will be implemented in task T4-3.
 */
@Service
public class DefaultResponseFormatter implements ResponseFormatter {

    @Override
    public RagQueryResponse format(String answer, List<Document> context, Double topRerankScore) {
        RagQueryResponse response = new RagQueryResponse();
        response.setAnswer(answer);
        response.setReferences(Collections.emptyList());
        response.setConfidence(topRerankScore != null ? topRerankScore : 0.0d);
        return response;
    }
}
