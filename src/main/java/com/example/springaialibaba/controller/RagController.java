package com.example.springaialibaba.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springaialibaba.controller.dto.RagQueryRequest;
import com.example.springaialibaba.controller.dto.RagQueryResponse;
import com.example.springaialibaba.formatter.ResponseFormatter;
import com.example.springaialibaba.generation.GenerationService;
import com.example.springaialibaba.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.retrieval.RetrievalService;

/**
 * REST controller orchestrating the RAG pipeline.
 */
@RestController
@RequestMapping(path = "/api/v1/rag", produces = MediaType.APPLICATION_JSON_VALUE)
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private static final String DEFAULT_PERSONA = "default";

    private static final String DEFAULT_CHANNEL = "generic";

    private final QueryPreprocessor queryPreprocessor;

    private final RetrievalService retrievalService;

    private final GenerationService generationService;

    private final ResponseFormatter responseFormatter;

    public RagController(QueryPreprocessor queryPreprocessor, RetrievalService retrievalService,
            GenerationService generationService, ResponseFormatter responseFormatter) {
        this.queryPreprocessor = queryPreprocessor;
        this.retrievalService = retrievalService;
        this.generationService = generationService;
        this.responseFormatter = responseFormatter;
    }

    @PostMapping(path = "/query", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        String rawQuestion = request != null ? request.getQuestion() : null;
        if (!StringUtils.hasText(rawQuestion)) {
            throw new IllegalArgumentException("question must not be blank");
        }

        log.info("收到 RAG 查询请求，问题长度={}", rawQuestion.length());
        String cleanedQuestion = queryPreprocessor.process(rawQuestion);
        log.debug("预处理后的查询：{}", cleanedQuestion);

        List<Document> documents = retrievalService.retrieveAndRerank(cleanedQuestion);
        log.info("检索到 {} 条候选文档", documents.size());

        String persona = normaliseOptionalInput(request.getPersona(), DEFAULT_PERSONA);
        String channel = normaliseOptionalInput(request.getChannel(), DEFAULT_CHANNEL);

        String answer = generationService.generate(rawQuestion, documents, persona, channel);
        log.debug("生成的回答长度={}", answer != null ? answer.length() : 0);

        Double topScore = extractTopScore(documents);
        RagQueryResponse response = responseFormatter.format(answer, documents, topScore);
        return ResponseEntity.ok(response);
    }

    private String normaliseOptionalInput(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
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
