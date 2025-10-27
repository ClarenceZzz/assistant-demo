package com.example.springaialibaba.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.example.springaialibaba.rerank.RerankedDocument;
import com.example.springaialibaba.rerank.siliconflow.RerankClient;

/**
 * Service responsible for retrieving documents from the configured {@link VectorStore}.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final VectorStore vectorStore;

    private final RerankClient rerankClient;

    private final int defaultInitialTopK;

    private final int defaultFinalTopN;

    public RetrievalService(VectorStore vectorStore, RerankClient rerankClient,
            @Value("${app.retrieval.initial-top-k:20}") int defaultInitialTopK,
            @Value("${app.retrieval.final-top-n:5}") int defaultFinalTopN) {
        this.vectorStore = vectorStore;
        this.rerankClient = rerankClient;
        this.defaultInitialTopK = defaultInitialTopK;
        this.defaultFinalTopN = defaultFinalTopN;
    }

    public List<Document> retrieve(String query) {
        return retrieve(query, defaultInitialTopK);
    }

    public List<Document> retrieve(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents != null ? documents : Collections.emptyList();
    }

    public List<Document> retrieveAndRerank(String query) {
        return retrieveAndRerank(query, defaultFinalTopN);
    }

    public List<Document> retrieveAndRerank(String query, int finalTopN) {
        List<Document> initialDocuments = retrieve(query, Math.max(defaultInitialTopK, finalTopN));
        if (initialDocuments.isEmpty()) {
            return initialDocuments;
        }
        int targetSize = Math.min(Math.max(finalTopN, 0), initialDocuments.size());
        if (targetSize == 0) {
            return Collections.emptyList();
        }
        List<String> contents = initialDocuments.stream()
                .map(this::resolveDocumentContent)
                .collect(Collectors.toList());
        try {
            List<RerankedDocument> rerankedDocuments = rerankClient.rerank(query, contents);
            if (rerankedDocuments.isEmpty()) {
                return limitDocuments(initialDocuments, targetSize);
            }
            List<Document> sortedDocuments = new ArrayList<>();
            for (RerankedDocument rerankedDocument : rerankedDocuments) {
                int originalIndex = rerankedDocument.getOriginalIndex();
                if (originalIndex < 0 || originalIndex >= initialDocuments.size()) {
                    log.warn("忽略无效的 Rerank 索引：{}", originalIndex);
                    continue;
                }
                sortedDocuments.add(initialDocuments.get(originalIndex));
                if (sortedDocuments.size() == targetSize) {
                    break;
                }
            }
            if (sortedDocuments.isEmpty()) {
                return limitDocuments(initialDocuments, targetSize);
            }
            return sortedDocuments;
        }
        catch (RuntimeException ex) {
            log.warn("Rerank 流程失败，降级返回向量检索结果", ex);
            return limitDocuments(initialDocuments, targetSize);
        }
    }

    public int getDefaultInitialTopK() {
        return defaultInitialTopK;
    }

    public int getDefaultFinalTopN() {
        return defaultFinalTopN;
    }

    private List<Document> limitDocuments(List<Document> documents, int limit) {
        if (documents.size() <= limit) {
            return new ArrayList<>(documents);
        }
        return new ArrayList<>(documents.subList(0, limit));
    }

    private String resolveDocumentContent(Document document) {
        String text = document.getText();
        return text != null ? text : document.getFormattedContent();
    }
}
