package com.example.springaialibaba.config;

import com.example.springaialibaba.config.properties.RagRewriteProperties;
import com.example.springaialibaba.core.client.RerankClient;
import com.example.springaialibaba.core.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.core.rag.modules.CustomDocumentJoiner;
import com.example.springaialibaba.core.rag.modules.CustomDocumentPostProcessor;
import com.example.springaialibaba.core.rag.modules.CustomDocumentRetriever;
import com.example.springaialibaba.core.rag.modules.CustomQueryAugmenter;
import com.example.springaialibaba.core.rag.modules.CustomQueryExpander;
import com.example.springaialibaba.core.rag.modules.CustomQueryTransformer;
import com.example.springaialibaba.core.rag.modules.SafeRewriteQueryTransformer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Configuration
public class RAGAdvisorConfig {

    @Bean
    public QueryTransformer customQueryTransformer(QueryPreprocessor queryPreprocessor) {
        return new CustomQueryTransformer(queryPreprocessor);
    }

    @Bean
    public QueryTransformer safeRewriteQueryTransformer(ChatClient.Builder chatClientBuilder,
            ResourceLoader resourceLoader, RagRewriteProperties ragRewriteProperties) {
        PromptTemplate promptTemplate = loadPromptTemplate(resourceLoader, ragRewriteProperties.getPromptTemplate());
        QueryTransformer rewriteDelegate = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .promptTemplate(promptTemplate)
                .build();
        return new SafeRewriteQueryTransformer(rewriteDelegate, ragRewriteProperties.isEnabled());
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
            @Qualifier("customQueryTransformer") QueryTransformer queryTransformer,
            @Qualifier("safeRewriteQueryTransformer") QueryTransformer safeRewriteQueryTransformer,
            QueryExpander queryExpander, 
            DocumentRetriever documentRetriever, 
            DocumentJoiner documentJoiner,
            DocumentPostProcessor documentPostProcessor, 
            QueryAugmenter queryAugmenter) {

        // 链路顺序与执行阶段一一对应，顺序变化会直接影响最终检索与生成效果。
        return RetrievalAugmentationAdvisor.builder()
            .queryTransformers(queryTransformer, safeRewriteQueryTransformer)
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

    private PromptTemplate loadPromptTemplate(ResourceLoader resourceLoader, String templateConfig) {
        // 优先按资源路径加载模板；不存在时退化为直接使用传入文本。
        if (!StringUtils.hasText(templateConfig)) {
            throw new IllegalStateException("RAG 改写 Prompt 模板未配置，请设置 app.rag.rewrite.prompt-template");
        }

        Resource resource = resourceLoader.getResource(templateConfig);
        if (resource.exists()) {
            return new PromptTemplate(resource);
        }
        return new PromptTemplate(templateConfig);
    }
}
