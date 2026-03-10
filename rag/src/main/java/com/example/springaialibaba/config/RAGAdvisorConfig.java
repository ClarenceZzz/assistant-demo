package com.example.springaialibaba.config;

import java.util.List;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGAdvisorConfig {
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(QueryTransformer queryTransformer,
            QueryExpander queryExpander, DocumentRetriever documentRetriever, DocumentJoiner documentJoiner,
            DocumentPostProcessor documentPostProcessor, QueryAugmenter queryAugmenter) {
        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(queryTransformer)
            .queryExpander(queryExpander)
            .documentRetriever(documentRetriever)
            .documentJoiner(documentJoiner)
            .documentPostProcessors(documentPostProcessor)
            .queryAugmenter(queryAugmenter)
            .build();
    }
}
