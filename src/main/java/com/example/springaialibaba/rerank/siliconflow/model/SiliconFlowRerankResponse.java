package com.example.springaialibaba.rerank.siliconflow.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SiliconFlow Rerank API 响应体。
 */
public class SiliconFlowRerankResponse {

    private String id;

    private List<Result> results;

    private Meta meta;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public static class Result {

        private Integer index;

        @JsonProperty("relevance_score")
        private Double relevanceScore;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
    }

    public static class Meta {

        @JsonProperty("billed_units")
        private BilledUnits billedUnits;

        private Tokens tokens;

        public BilledUnits getBilledUnits() {
            return billedUnits;
        }

        public void setBilledUnits(BilledUnits billedUnits) {
            this.billedUnits = billedUnits;
        }

        public Tokens getTokens() {
            return tokens;
        }

        public void setTokens(Tokens tokens) {
            this.tokens = tokens;
        }
    }

    public static class BilledUnits {

        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;

        @JsonProperty("search_units")
        private Integer searchUnits;

        private Integer classifications;

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }

        public Integer getSearchUnits() {
            return searchUnits;
        }

        public void setSearchUnits(Integer searchUnits) {
            this.searchUnits = searchUnits;
        }

        public Integer getClassifications() {
            return classifications;
        }

        public void setClassifications(Integer classifications) {
            this.classifications = classifications;
        }
    }

    public static class Tokens {

        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
        }
    }
}
