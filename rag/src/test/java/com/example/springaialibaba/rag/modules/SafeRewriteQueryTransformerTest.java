package com.example.springaialibaba.rag.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.RagQueryContext;
import com.example.springaialibaba.core.rag.modules.SafeRewriteQueryTransformer;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

@ExtendWith(MockitoExtension.class)
class SafeRewriteQueryTransformerTest {

    @Mock
    private QueryTransformer delegate;

    private Query query;

    @BeforeEach
    void setUp() {
        query = Query.builder()
                .text("raw query")
                .context(Map.of(
                        RagQueryContext.ORIGINAL_QUESTION, "原始问题",
                        RagQueryContext.CLEANED_QUESTION, "清洗问题",
                        "documentSource", "faq"))
                .build();
    }

    @Test
    void shouldRewriteQueryAndPreserveContext() {
        SafeRewriteQueryTransformer transformer = new SafeRewriteQueryTransformer(delegate, true);
        when(delegate.transform(query)).thenReturn(Query.builder()
                .text("改写后的检索问题")
                .context(Map.of("ignored", "value"))
                .build());

        Query transformed = transformer.transform(query);

        assertThat(transformed.text()).isEqualTo("改写后的检索问题");
        assertThat(transformed.context()).containsEntry(RagQueryContext.ORIGINAL_QUESTION, "原始问题");
        assertThat(transformed.context()).containsEntry(RagQueryContext.CLEANED_QUESTION, "清洗问题");
        assertThat(transformed.context()).containsEntry(RagQueryContext.REWRITTEN_QUESTION, "改写后的检索问题");
        assertThat(transformed.context()).containsEntry("documentSource", "faq");
    }

    @Test
    void shouldFallbackToCleanedQuestionWhenDelegateThrows() {
        // 改写异常时必须降级到 cleanedQuestion，避免影响后续检索链路稳定性。
        SafeRewriteQueryTransformer transformer = new SafeRewriteQueryTransformer(delegate, true);
        when(delegate.transform(query)).thenThrow(new RuntimeException("rewrite failed"));

        Query transformed = transformer.transform(query);

        assertThat(transformed.text()).isEqualTo("清洗问题");
        assertThat(transformed.context()).containsEntry(RagQueryContext.REWRITTEN_QUESTION, "清洗问题");
    }

    @Test
    void shouldFallbackToCleanedQuestionWhenDelegateReturnsBlank() {
        SafeRewriteQueryTransformer transformer = new SafeRewriteQueryTransformer(delegate, true);
        Query rewrittenQuery = org.mockito.Mockito.mock(Query.class);
        when(rewrittenQuery.text()).thenReturn("   ");
        when(delegate.transform(query)).thenReturn(rewrittenQuery);

        Query transformed = transformer.transform(query);

        assertThat(transformed.text()).isEqualTo("清洗问题");
        assertThat(transformed.context()).containsEntry(RagQueryContext.REWRITTEN_QUESTION, "清洗问题");
    }

    @Test
    void shouldSkipDelegateWhenRewriteDisabled() {
        // 关闭开关后应短路 delegate，直接复用清洗后的问题。
        SafeRewriteQueryTransformer transformer = new SafeRewriteQueryTransformer(delegate, false);

        Query transformed = transformer.transform(query);

        assertThat(transformed.text()).isEqualTo("清洗问题");
        assertThat(transformed.context()).containsEntry(RagQueryContext.REWRITTEN_QUESTION, "清洗问题");
        verify(delegate, never()).transform(query);
    }
}
