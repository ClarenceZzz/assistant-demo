package com.example.springaialibaba.rerank.siliconflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.example.springaialibaba.rerank.RerankedDocument;
import com.example.springaialibaba.rerank.siliconflow.model.SiliconFlowRerankRequest;
import com.example.springaialibaba.rerank.siliconflow.model.SiliconFlowRerankResponse;
import com.example.springaialibaba.rerank.siliconflow.model.SiliconFlowRerankResponse.Result;

/**
 * 调用 SiliconFlow Rerank API 的客户端。
 */
@Service
public class RerankClient {

    private static final Logger log = LoggerFactory.getLogger(RerankClient.class);

    private final RestTemplate restTemplate;

    private final SiliconFlowRerankProperties properties;

    public RerankClient(RestTemplate restTemplate, SiliconFlowRerankProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 根据查询和候选文档调用 Rerank API 并返回排序结果。
     * @param query 用户查询
     * @param documents 候选文档列表
     * @return 排序后的文档结果
     */
    public List<RerankedDocument> rerank(String query, List<String> documents) {
        Assert.hasText(query, "query 不能为空");
        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }
        SiliconFlowRerankRequest request = buildRequest(query, documents);
        ResponseEntity<SiliconFlowRerankResponse> response = invokeApi(request);
        SiliconFlowRerankResponse body = response.getBody();
        if (body == null) {
            throw new SiliconFlowRerankException("SiliconFlow Rerank 响应体为空", response.getStatusCode(), null, null);
        }
        return mapResults(body.getResults(), documents);
    }

    private SiliconFlowRerankRequest buildRequest(String query, List<String> documents) {
        SiliconFlowRerankRequest request = new SiliconFlowRerankRequest();
        request.setModel(properties.getModel());
        request.setInstruction(properties.getInstruction());
        request.setQuery(query);
        request.setDocuments(documents);
        Integer resolvedTopN = resolveTopN(documents.size());
        if (resolvedTopN != null) {
            request.setTopN(resolvedTopN);
        }
        request.setReturnDocuments(properties.getReturnDocuments());
        request.setMaxChunksPerDoc(properties.getMaxChunksPerDoc());
        request.setOverlapTokens(properties.getOverlapTokens());
        return request;
    }

    private ResponseEntity<SiliconFlowRerankResponse> invokeApi(SiliconFlowRerankRequest payload) {
        if (!StringUtils.hasText(properties.getApiUrl())) {
            throw new IllegalStateException("spring.ai.siliconflow.rerank.api-url 未配置");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("spring.ai.siliconflow.rerank.api-key 未配置");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        HttpEntity<SiliconFlowRerankRequest> entity = new HttpEntity<>(payload, headers);
        try {
            return restTemplate.postForEntity(properties.getApiUrl(), entity, SiliconFlowRerankResponse.class);
        }
        catch (RestClientResponseException ex) {
            log.error("调用 SiliconFlow Rerank API 失败，状态码 {}，响应体 {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new SiliconFlowRerankException("调用 SiliconFlow Rerank API 失败", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        }
        catch (RestClientException ex) {
            log.error("调用 SiliconFlow Rerank API 出现异常", ex);
            throw new SiliconFlowRerankException("调用 SiliconFlow Rerank API 出现异常", ex);
        }
    }

    private List<RerankedDocument> mapResults(List<Result> results, List<String> documents) {
        if (CollectionUtils.isEmpty(results)) {
            return Collections.emptyList();
        }
        List<RerankedDocument> rerankedDocuments = new ArrayList<>(results.size());
        for (Result result : results) {
            if (result == null || result.getIndex() == null) {
                log.warn("Rerank 结果缺少 index 字段：{}", result);
                continue;
            }
            int index = result.getIndex();
            if (index < 0 || index >= documents.size()) {
                log.warn("Rerank 结果 index 越界：{}，输入文档总数 {}", index, documents.size());
                continue;
            }
            String content = documents.get(index);
            double score = result.getRelevanceScore() != null ? result.getRelevanceScore() : 0.0d;
            rerankedDocuments.add(new RerankedDocument(index, content, score));
        }
        return rerankedDocuments;
    }

    private Integer resolveTopN(int documentCount) {
        Integer topN = properties.getTopN();
        if (topN == null) {
            return null;
        }
        return Math.min(Math.max(topN, 1), documentCount);
    }
}
