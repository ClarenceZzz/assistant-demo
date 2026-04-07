package com.example.springaialibaba.config;

import com.example.springaialibaba.core.rag.modules.RoutingQueryTransformer;
import com.example.springaialibaba.core.rag.query.DocumentQueryExecutorFactory;
import com.example.springaialibaba.core.rag.query.EsKeywordDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.query.MysqlDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.query.PgVectorDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.query.RoutedDocumentQueryService;
import com.example.springaialibaba.core.rag.routing.KeywordQueryRouter;
import com.example.springaialibaba.core.rag.routing.LlmQueryRouter;
import com.example.springaialibaba.core.rag.routing.QueryRouter;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class RoutingConfig {

    private static final String ROUTING_PROMPT_RESOURCE = "classpath:prompts/rag_route_prompt.txt";

    @Bean("routingChatClient")
    public ChatClient routingChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public KeywordQueryRouter keywordQueryRouter() {
        return new KeywordQueryRouter();
    }

    @Bean("llmQueryRouter")
    public QueryRouter llmQueryRouter(@Qualifier("routingChatClient") ChatClient routingChatClient,
            ResourceLoader resourceLoader,
            @Value("${app.rag.routing.default-route:PG_VECTOR}") String defaultRouteValue) {
        RouteKey defaultRoute = RouteKey.fromValueOrDefault(defaultRouteValue, RouteKey.PG_VECTOR);
        return new LlmQueryRouter(routingChatClient, loadPrompt(resourceLoader), defaultRoute);
    }

    @Bean("routingQueryTransformer")
    public QueryTransformer routingQueryTransformer(@Qualifier("llmQueryRouter") QueryRouter llmQueryRouter,
            @Value("${app.rag.routing.enabled:true}") boolean routingEnabled,
            @Value("${app.rag.routing.llm-enabled:true}") boolean llmEnabled,
            @Value("${app.rag.routing.default-route:PG_VECTOR}") String defaultRouteValue) {
        return new RoutingQueryTransformer(llmQueryRouter, routingEnabled, llmEnabled,
                RouteKey.fromValueOrDefault(defaultRouteValue, RouteKey.PG_VECTOR));
    }

    @Bean
    public PgVectorDocumentQueryExecutor pgVectorDocumentQueryExecutor(VectorStore vectorStore,
            @Value("${app.retrieval.initial-top-k:20}") int topK,
            @Value("${app.retrieval.similarity-threshold:0.0}") double similarityThreshold) {
        return new PgVectorDocumentQueryExecutor(vectorStore, topK, similarityThreshold);
    }

    @Bean
    public MysqlDocumentQueryExecutor mysqlDocumentQueryExecutor() {
        return new MysqlDocumentQueryExecutor();
    }

    @Bean
    public EsKeywordDocumentQueryExecutor esKeywordDocumentQueryExecutor() {
        return new EsKeywordDocumentQueryExecutor();
    }

    @Bean
    public DocumentQueryExecutorFactory documentQueryExecutorFactory(
            PgVectorDocumentQueryExecutor pgVectorDocumentQueryExecutor,
            MysqlDocumentQueryExecutor mysqlDocumentQueryExecutor,
            EsKeywordDocumentQueryExecutor esKeywordDocumentQueryExecutor) {
        return new DocumentQueryExecutorFactory(pgVectorDocumentQueryExecutor, mysqlDocumentQueryExecutor,
                esKeywordDocumentQueryExecutor);
    }

    @Bean
    public RoutedDocumentQueryService routedDocumentQueryService(
            DocumentQueryExecutorFactory documentQueryExecutorFactory,
            @Value("${app.rag.routing.default-route:PG_VECTOR}") String defaultRouteValue) {
        return new RoutedDocumentQueryService(documentQueryExecutorFactory,
                RouteKey.fromValueOrDefault(defaultRouteValue, RouteKey.PG_VECTOR));
    }

    private String loadPrompt(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource(ROUTING_PROMPT_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to load routing prompt: " + ROUTING_PROMPT_RESOURCE, ex);
        }
    }
}
