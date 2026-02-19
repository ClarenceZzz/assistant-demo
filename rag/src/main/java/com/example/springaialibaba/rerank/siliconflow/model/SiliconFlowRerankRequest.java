package com.example.springaialibaba.rerank.siliconflow.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SiliconFlow Rerank API 请求体。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiliconFlowRerankRequest {

    private String model;

    private String instruction;

    private String query;

    private List<String> documents;

    @JsonProperty("top_n")
    private Integer topN;

    @JsonProperty("return_documents")
    private Boolean returnDocuments;

    @JsonProperty("max_chunks_per_doc")
    private Integer maxChunksPerDoc;

    @JsonProperty("overlap_tokens")
    private Integer overlapTokens;

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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents;
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
