package com.example.springaialibaba.rerank;

/**
 * 表示经 Rerank API 排序后的文档结果。
 */
public class RerankedDocument {

    private final int originalIndex;

    private final String content;

    private final double relevanceScore;

    public RerankedDocument(int originalIndex, String content, double relevanceScore) {
        this.originalIndex = originalIndex;
        this.content = content;
        this.relevanceScore = relevanceScore;
    }

    public int getOriginalIndex() {
        return originalIndex;
    }

    public String getContent() {
        return content;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }
}
