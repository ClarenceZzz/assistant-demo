package com.example.springaialibaba.rag.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.core.rag.RagMetadataFilterContext;
import com.example.springaialibaba.core.rag.modules.CustomQueryTransformer;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.rag.Query;

@ExtendWith(MockitoExtension.class)
class CustomQueryTransformerTest {

    @Mock
    private QueryPreprocessor queryPreprocessor;

    private CustomQueryTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new CustomQueryTransformer(queryPreprocessor);
    }

    @Test
    void shouldNormaliseMetadataFiltersIntoQueryContext() {
        when(queryPreprocessor.process("  raw query  ")).thenReturn("raw query");

        Query query = Query.builder()
                .text("  raw query  ")
                .context(Map.of(
                        RagMetadataFilterContext.DOCUMENT_SOURCE, " faq ",
                        RagMetadataFilterContext.DOCUMENT_TYPE, " pdf ",
                        RagMetadataFilterContext.DATE_FROM, " 2025-01-01 ",
                        RagMetadataFilterContext.DATE_TO, "2025-12-31",
                        RagMetadataFilterContext.FILTERS, Map.of(" region ", " cn ", "product", " ev ")))
                .build();

        Query transformed = transformer.transform(query);

        assertThat(transformed.text()).isEqualTo("raw query");
        assertThat(transformed.context()).containsEntry("originalQuestion", "  raw query  ");
        assertThat(transformed.context()).containsEntry(RagMetadataFilterContext.DOCUMENT_SOURCE, "faq");
        assertThat(transformed.context()).containsEntry(RagMetadataFilterContext.DOCUMENT_TYPE, "pdf");
        assertThat(transformed.context()).containsEntry(RagMetadataFilterContext.DATE_FROM, "2025-01-01");
        assertThat(transformed.context()).containsEntry(RagMetadataFilterContext.DATE_TO, "2025-12-31");
        assertThat(transformed.context().get(RagMetadataFilterContext.FILTERS))
                .isEqualTo(Map.of("region", "cn", "product", "ev"));
    }

    @Test
    void shouldDropInvalidDateFilters() {
        when(queryPreprocessor.process("query")).thenReturn("query");

        Query query = Query.builder()
                .text("query")
                .context(Map.of(
                        RagMetadataFilterContext.DATE_FROM, "not-a-date",
                        RagMetadataFilterContext.DATE_TO, "2025-02-01"))
                .build();

        Query transformed = transformer.transform(query);

        assertThat(transformed.context()).doesNotContainKey(RagMetadataFilterContext.DATE_FROM);
        assertThat(transformed.context()).containsEntry(RagMetadataFilterContext.DATE_TO, "2025-02-01");
    }
}
