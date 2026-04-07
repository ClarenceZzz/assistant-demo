package com.example.springaialibaba.controller;

import com.example.springaialibaba.core.formatter.ResponseFormatter;
import com.example.springaialibaba.core.rag.RagMetadataFilterContext;
import com.example.springaialibaba.core.rag.RagQueryContext;
import com.example.springaialibaba.core.rag.modules.RoutingQueryTransformer;
import com.example.springaialibaba.core.rag.routing.KeywordQueryRouter;
import com.example.springaialibaba.core.rag.routing.RouteRequest;
import com.example.springaialibaba.core.rag.routing.RouterDecision;
import com.example.springaialibaba.model.dto.RagQueryRequest;
import com.example.springaialibaba.model.dto.RagQueryResponse;
import com.example.springaialibaba.utils.RagValueUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller orchestrating the modular Advisor-based RAG pipeline.
 */
@RestController
@RequestMapping(path = "/api/v1/rag/modular", produces = MediaType.APPLICATION_JSON_VALUE)
public class ModularRagController {

    private static final Logger log = LoggerFactory.getLogger(ModularRagController.class);
    private static final String REPAIR_ROUTE_HINT = "repair";
    private static final List<String> REPAIR_ROUTE_HINT_KEYWORDS =
            List.of("报修", "故障报修", "维修单", "售后报修");
    private static final String DEFAULT_PERSONA = "客服人员";
    private static final String DEFAULT_CHANNEL = "售后服务";

    private final ChatClient chatClient;
    private final ResponseFormatter responseFormatter;
    private final KeywordQueryRouter keywordQueryRouter;

    public ModularRagController(@Qualifier("ragChatClient") ChatClient chatClient,
            ResponseFormatter responseFormatter,
            KeywordQueryRouter keywordQueryRouter) {
        this.chatClient = chatClient;
        this.responseFormatter = responseFormatter;
        this.keywordQueryRouter = keywordQueryRouter;
    }

    @PostMapping(path = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        String rawQuestion = request != null ? request.getQuestion() : null;
        if (!StringUtils.hasText(rawQuestion)) {
            throw new IllegalArgumentException("question must not be blank");
        }

        String persona = RagValueUtils.trimOrDefault(request.getPersona(), DEFAULT_PERSONA);
        String channel = RagValueUtils.trimOrDefault(request.getChannel(), DEFAULT_CHANNEL);

        ChatClientResponse advisorResponse = chatClient.prompt()
                .advisors(spec -> {
                    spec.param(RagQueryContext.ORIGINAL_QUESTION, rawQuestion)
                            .param(RagQueryContext.PERSONA, persona)
                            .param(RagQueryContext.CHANNEL, channel);
                    applyMetadataFilterParams(spec, request);
                    applyRouteKeyHint(spec, rawQuestion);
                    applyRouteHint(spec, rawQuestion);
                })
                .user(rawQuestion)
                .call()
                .chatClientResponse();

        String answer = advisorResponse != null ? extractAnswer(advisorResponse.chatResponse()) : "";
        List<Document> documents = extractDocuments(advisorResponse != null ? advisorResponse.context() : null);
        log.info("Modular Advisor 链路完成，检索文档数={}，回答长度={}", documents.size(),
                answer != null ? answer.length() : 0);

        Double topScore = RagValueUtils.extractTopScore(documents, log);
        RagQueryResponse response = responseFormatter.format(answer, documents, topScore);
        return ResponseEntity.ok(response);
    }

    private String extractAnswer(ChatResponse chatResponse) {
        if (chatResponse == null
                || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null
                || !StringUtils.hasText(chatResponse.getResult().getOutput().getText())) {
            return "";
        }
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * 从 Advisor 上下文中提取检索文档，仅保留 Document 类型对象。
     */
    private List<Document> extractDocuments(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return List.of();
        }
        Object rawDocuments = context.get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (!(rawDocuments instanceof List<?> rawList)) {
            return List.of();
        }
        List<Document> documents = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            if (item instanceof Document document) {
                documents.add(document);
            }
        }
        return documents;
    }

    /**
     * 只透传有文本内容的过滤参数，避免空值进入检索上下文。
     */
    private void applyMetadataFilterParams(ChatClient.AdvisorSpec advisorSpec, RagQueryRequest request) {
        if (request == null) {
            return;
        }
        putAdvisorParamIfHasText(advisorSpec, RagMetadataFilterContext.DOCUMENT_SOURCE, request.getDocumentSource());
        putAdvisorParamIfHasText(advisorSpec, RagMetadataFilterContext.DOCUMENT_TYPE, request.getDocumentType());
        putAdvisorParamIfHasText(advisorSpec, RagMetadataFilterContext.DATE_FROM, request.getDateFrom());
        putAdvisorParamIfHasText(advisorSpec, RagMetadataFilterContext.DATE_TO, request.getDateTo());

        if (request.getFilters() == null || request.getFilters().isEmpty()) {
            return;
        }

        Map<String, String> sanitizedFilters = new LinkedHashMap<>();
        request.getFilters().forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                sanitizedFilters.put(key.trim(), value.trim());
            }
        });
        if (!sanitizedFilters.isEmpty()) {
            advisorSpec.param(RagMetadataFilterContext.FILTERS, sanitizedFilters);
        }
    }

    private void applyRouteKeyHint(ChatClient.AdvisorSpec advisorSpec, String rawQuestion) {
        RouterDecision routeKeyHintDecision = keywordQueryRouter.route(new RouteRequest(rawQuestion, null));
        if (routeKeyHintDecision != null && routeKeyHintDecision.hasRoute()) {
            advisorSpec.param(RoutingQueryTransformer.ROUTE_KEY_HINT_CONTEXT_KEY,
                    routeKeyHintDecision.getRouteKey().name());
            log.info("Pre-routing hit: routeKeyHint={}, confidence={}, reason={}",
                    routeKeyHintDecision.getRouteKey(), routeKeyHintDecision.getConfidence(),
                    routeKeyHintDecision.getReason());
        }
    }

    private void applyRouteHint(ChatClient.AdvisorSpec advisorSpec, String rawQuestion) {
        if (!StringUtils.hasText(rawQuestion)) {
            return;
        }
        for (String keyword : REPAIR_ROUTE_HINT_KEYWORDS) {
            if (rawQuestion.contains(keyword)) {
                advisorSpec.param(RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY, REPAIR_ROUTE_HINT);
                log.info("Business route hint hit: routeHint={}, keyword={}", REPAIR_ROUTE_HINT, keyword);
                return;
            }
        }
    }

    private void putAdvisorParamIfHasText(ChatClient.AdvisorSpec advisorSpec, String key, String value) {
        if (StringUtils.hasText(value)) {
            advisorSpec.param(key, value.trim());
        }
    }
}
