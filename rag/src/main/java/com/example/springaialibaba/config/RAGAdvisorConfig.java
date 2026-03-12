package com.example.springaialibaba.config;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.springaialibaba.core.rag.modules.CustomDocumentJoiner;
import com.example.springaialibaba.core.rag.modules.CustomDocumentPostProcessor;
import com.example.springaialibaba.core.rag.modules.CustomDocumentRetriever;
import com.example.springaialibaba.core.rag.modules.CustomQueryAugmenter;
import com.example.springaialibaba.core.rag.modules.CustomQueryExpander;
import com.example.springaialibaba.core.rag.modules.CustomQueryTransformer;

@Configuration
public class RAGAdvisorConfig {

    @Bean
    public QueryTransformer customQueryTransformer() {
        return new CustomQueryTransformer();
    }

    @Bean
    public QueryExpander customQueryExpander() {
        return new CustomQueryExpander();
    }

    @Bean
    public DocumentRetriever customDocumentRetriever() {
        // 后续可以通过参数将 VectorStore 或外部服务依赖注入到这里
        return new CustomDocumentRetriever();
    }

    @Bean
    public DocumentJoiner customDocumentJoiner() {
        return new CustomDocumentJoiner();
    }

    @Bean
    public DocumentPostProcessor customDocumentPostProcessor() {
        return new CustomDocumentPostProcessor();
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
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                .topK(5)
                .build())
            .documentJoiner(documentJoiner)
            .documentPostProcessors(documentPostProcessor)
            .queryAugmenter(queryAugmenter)
            .build();
    }
}
