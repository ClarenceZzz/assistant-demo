package com.example.springaialibaba.config;

import com.example.springaialibaba.core.client.RerankClient;
import com.example.springaialibaba.core.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.core.rag.modules.CustomDocumentJoiner;
import com.example.springaialibaba.core.rag.modules.CustomDocumentPostProcessor;
import com.example.springaialibaba.core.rag.modules.CustomDocumentRetriever;
import com.example.springaialibaba.core.rag.modules.CustomQueryAugmenter;
import com.example.springaialibaba.core.rag.modules.CustomQueryExpander;
import com.example.springaialibaba.core.rag.modules.CustomQueryTransformer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RAGAdvisorConfig {

    @Bean
    public QueryTransformer customQueryTransformer(QueryPreprocessor queryPreprocessor) {
        return new CustomQueryTransformer(queryPreprocessor);
    }

    @Bean
    public QueryExpander customQueryExpander() {
        return new CustomQueryExpander();
    }

    @Bean
    public DocumentRetriever customDocumentRetriever(
            VectorStore vectorStore,
            @Value("${app.retrieval.initial-top-k:20}") int topK,
            @Value("${app.retrieval.similarity-threshold:0.0}") double similarityThreshold) {
        return new CustomDocumentRetriever(vectorStore, topK, similarityThreshold);
    }

    @Bean
    public DocumentJoiner customDocumentJoiner() {
        return new CustomDocumentJoiner();
    }

    @Bean
    public DocumentPostProcessor customDocumentPostProcessor(
            RerankClient rerankClient,
            @Value("${app.retrieval.final-top-n:5}") int topN) {
        return new CustomDocumentPostProcessor(rerankClient, topN);
    }

    @Bean
    public QueryAugmenter customQueryAugmenter() {
        return new CustomQueryAugmenter();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            QueryTransformer queryTransformer,
            QueryExpander queryExpander, 
            DocumentRetriever documentRetriever, 
            DocumentJoiner documentJoiner,
            DocumentPostProcessor documentPostProcessor, 
            QueryAugmenter queryAugmenter) {
        
        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(queryTransformer)
            .queryExpander(queryExpander)
            .documentRetriever(documentRetriever)
            .documentJoiner(documentJoiner)
            .documentPostProcessors(documentPostProcessor)
            .queryAugmenter(queryAugmenter)
            .build();
    }

    @Bean("ragChatClient")
    public ChatClient ragChatClient(OpenAiChatModel chatModel,
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }
}
