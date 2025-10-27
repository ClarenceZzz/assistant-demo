package com.example.springaialibaba.retrieval;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * Service responsible for retrieving documents from the configured {@link VectorStore}.
 */
@Service
public class RetrievalService {

    private final VectorStore vectorStore;

    private final int defaultTopK;

    public RetrievalService(VectorStore vectorStore,
            @Value("${app.retrieval.initial-top-k:20}") int defaultTopK) {
        this.vectorStore = vectorStore;
        this.defaultTopK = defaultTopK;
    }

    public List<Document> retrieve(String query) {
        return retrieve(query, defaultTopK);
    }

    public List<Document> retrieve(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents != null ? documents : Collections.emptyList();
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }
}
