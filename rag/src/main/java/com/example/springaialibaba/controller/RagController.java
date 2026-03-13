package com.example.springaialibaba.controller;

import java.util.ArrayList;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springaialibaba.model.dto.RagQueryRequest;
import com.example.springaialibaba.model.dto.RagQueryResponse;
import com.example.springaialibaba.core.formatter.ResponseFormatter;

/**
 * REST controller orchestrating the RAG pipeline.
 */
@RestController
@RequestMapping(path = "/api/v1/rag", produces = MediaType.APPLICATION_JSON_VALUE)
public class RagController {
    private static final Logger log = LoggerFactory.getLogger(RagController.class);
    private static final String DEFAULT_PERSONA = "客服人员";
    private static final String DEFAULT_CHANNEL = "售后服务";

    private final ChatClient chatClient;
    private final ResponseFormatter responseFormatter;

    public RagController(@Qualifier("ragChatClient") ChatClient chatClient,
            ResponseFormatter responseFormatter) {
        this.chatClient = chatClient;
        this.responseFormatter = responseFormatter;
    }

    @PostMapping(path = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        String rawQuestion = request != null ? request.getQuestion() : null;
        if (!StringUtils.hasText(rawQuestion)) {
            throw new IllegalArgumentException("question must not be blank");
        }

        String persona = normaliseOptionalInput(request.getPersona(), DEFAULT_PERSONA);
        String channel = normaliseOptionalInput(request.getChannel(), DEFAULT_CHANNEL);

        ChatClientResponse advisorResponse = chatClient.prompt()
                .advisors(spec -> spec
                        .param("originalQuestion", rawQuestion)
                        .param("persona", persona)
                        .param("channel", channel))
                .user(rawQuestion)
                .call()
                .chatClientResponse();

        String answer = advisorResponse != null ? extractAnswer(advisorResponse.chatResponse()) : "";
        List<Document> documents = extractDocuments(advisorResponse != null ? advisorResponse.context() : null);
        log.info("Advisor 链路完成，检索文档数={}，回答长度={}", documents.size(),
                answer != null ? answer.length() : 0);

        Double topScore = extractTopScore(documents);
        RagQueryResponse response = responseFormatter.format(answer, documents, topScore);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/query-new", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void queryNew() {
        String deviceType = "";
        String userId = "";
    }

    private String normaliseOptionalInput(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
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

    private Double extractTopScore(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return null;
        }
        Document first = documents.get(0);
        Map<String, Object> metadata = first.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object rawScore = metadata.get("score");
        if (rawScore == null) {
            rawScore = metadata.get("rerank_score");
        }
        if (rawScore instanceof Number) {
            return ((Number) rawScore).doubleValue();
        }
        if (rawScore instanceof String) {
            try {
                return Double.parseDouble(((String) rawScore));
            }
            catch (NumberFormatException ignored) {
                log.debug("无法解析 rerank 分数：{}", rawScore);
            }
        }
        return null;
    }
}
