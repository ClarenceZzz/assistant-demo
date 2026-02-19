package com.example.springaialibaba.rerank.siliconflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SiliconFlow Rerank 客户端配置属性。
 */
@ConfigurationProperties(prefix = "spring.ai.siliconflow.rerank")
public class SiliconFlowRerankProperties {

    private String apiUrl = "https://api.siliconflow.cn/v1/rerank";

    private String apiKey;

    private String model = "Qwen/Qwen3-Reranker-8B";

    private String instruction = "Please rerank the documents based on the query.";

    private Integer topN = 5;

    private Boolean returnDocuments = Boolean.TRUE;

    private Integer maxChunksPerDoc;

    private Integer overlapTokens;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }

    public Boolean getReturnDocuments() {
        return returnDocuments;
    }

    public void setReturnDocuments(Boolean returnDocuments) {
        this.returnDocuments = returnDocuments;
    }

    public Integer getMaxChunksPerDoc() {
        return maxChunksPerDoc;
    }

    public void setMaxChunksPerDoc(Integer maxChunksPerDoc) {
        this.maxChunksPerDoc = maxChunksPerDoc;
    }

    public Integer getOverlapTokens() {
        return overlapTokens;
    }

    public void setOverlapTokens(Integer overlapTokens) {
        this.overlapTokens = overlapTokens;
    }
}
