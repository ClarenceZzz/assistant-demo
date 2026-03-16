package com.example.springaialibaba.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

class RagValueUtilsTest {

    @Test
    void shouldTrimToNullForNullBlankAndObject() {
        assertThat(RagValueUtils.trimToNull(null)).isNull();
        assertThat(RagValueUtils.trimToNull("   ")).isNull();
        assertThat(RagValueUtils.trimToNull("  value  ")).isEqualTo("value");
        assertThat(RagValueUtils.trimToNull(123)).isEqualTo("123");
    }

    @Test
    void shouldTrimOrUseDefault() {
        assertThat(RagValueUtils.trimOrDefault("  x  ", "default")).isEqualTo("x");
        assertThat(RagValueUtils.trimOrDefault("   ", "default")).isEqualTo("default");
        assertThat(RagValueUtils.trimOrDefault(null, "default")).isEqualTo("default");
    }

    @Test
    void shouldExtractTopScoreFromScore() {
        Logger log = mock(Logger.class);
        List<Document> documents = List.of(new Document("doc", Map.of("score", 0.85)));

        Double score = RagValueUtils.extractTopScore(documents, log);

        assertThat(score).isEqualTo(0.85d);
    }

    @Test
    void shouldExtractTopScoreFromRerankScoreString() {
        Logger log = mock(Logger.class);
        List<Document> documents = List.of(new Document("doc", Map.of("rerank_score", "0.92")));

        Double score = RagValueUtils.extractTopScore(documents, log);

        assertThat(score).isEqualTo(0.92d);
    }

    @Test
    void shouldReturnNullAndLogWhenScoreIsInvalid() {
        Logger log = mock(Logger.class);
        List<Document> documents = List.of(new Document("doc", Map.of("rerank_score", "invalid")));

        Double score = RagValueUtils.extractTopScore(documents, log);

        assertThat(score).isNull();
        verify(log).debug("无法解析 rerank 分数：{}", "invalid");
    }

    @Test
    void shouldReturnNullWhenDocumentsOrMetadataEmpty() {
        Logger log = mock(Logger.class);

        assertThat(RagValueUtils.extractTopScore(List.of(), log)).isNull();
        assertThat(RagValueUtils.extractTopScore(List.of(new Document("doc", Map.of())), log)).isNull();
    }

    @Test
    void shouldResolveContextValueTrimmed() {
        Map<String, Object> context = Map.of("k", "  value  ");

        assertThat(RagValueUtils.resolveContextValueTrimmed(context, "k", " default ")).isEqualTo("value");
        assertThat(RagValueUtils.resolveContextValueTrimmed(context, "missing", " default ")).isEqualTo("default");
        assertThat(RagValueUtils.resolveContextValueTrimmed(context, "missing", null)).isEqualTo("");
    }

    @Test
    void shouldResolveContextValuePreserve() {
        Query query = Query.builder().text("question").context(Map.of("k", "  value  ")).build();

        assertThat(RagValueUtils.resolveContextValuePreserve(query, "k", " default ")).isEqualTo("  value  ");
        assertThat(RagValueUtils.resolveContextValuePreserve(query, "missing", " default ")).isEqualTo(" default ");
    }

}
