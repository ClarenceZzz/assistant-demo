package com.example.springaialibaba.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.test.util.ReflectionTestUtils;

class RAGAdvisorConfigTest {

    @Test
    void shouldRegisterCleansingAndRewriteTransformersInOrder() {
        RAGAdvisorConfig config = new RAGAdvisorConfig();
        QueryTransformer cleansingTransformer = mock(QueryTransformer.class);
        QueryTransformer rewriteTransformer = mock(QueryTransformer.class);
        QueryExpander queryExpander = mock(QueryExpander.class);
        DocumentRetriever documentRetriever = mock(DocumentRetriever.class);
        DocumentJoiner documentJoiner = mock(DocumentJoiner.class);
        DocumentPostProcessor documentPostProcessor = mock(DocumentPostProcessor.class);
        QueryAugmenter queryAugmenter = mock(QueryAugmenter.class);

        RetrievalAugmentationAdvisor advisor = config.retrievalAugmentationAdvisor(
                cleansingTransformer,
                rewriteTransformer,
                queryExpander,
                documentRetriever,
                documentJoiner,
                documentPostProcessor,
                queryAugmenter);

        @SuppressWarnings("unchecked")
        List<QueryTransformer> queryTransformers =
                (List<QueryTransformer>) ReflectionTestUtils.getField(advisor, "queryTransformers");

        assertThat(queryTransformers).containsExactly(cleansingTransformer, rewriteTransformer);
    }
}
