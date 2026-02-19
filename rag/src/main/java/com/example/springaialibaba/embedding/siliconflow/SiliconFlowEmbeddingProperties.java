package com.example.springaialibaba.embedding.siliconflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SiliconFlow Embedding 客户端配置。
 */
@ConfigurationProperties(prefix = "spring.ai.siliconflow")
public class SiliconFlowEmbeddingProperties {

    private String apiUrl = "https://api.siliconflow.cn/v1/embeddings";

    private String apiKey;

    private String model = "Qwen/Qwen3-Embedding-8B";

    private String encodingFormat = "float";

    private Integer dimensions = 1536;

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

    public String getEncodingFormat() {
        return encodingFormat;
    }

    public void setEncodingFormat(String encodingFormat) {
        this.encodingFormat = encodingFormat;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }
}
