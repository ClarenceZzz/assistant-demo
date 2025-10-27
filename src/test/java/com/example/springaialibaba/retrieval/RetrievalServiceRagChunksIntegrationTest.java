package com.example.springaialibaba.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test that verifies {@link RetrievalService} can read data from the
 * {@code rag_chunks} table configured in {@code application-test.yml}.
 */
@SpringBootTest
@ActiveProfiles("test")
class RetrievalServiceRagChunksIntegrationTest {

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private VectorStore vectorStore;

    @Test
    void shouldRetrieveChunkFromRagChunksTable() {
        Map<String, Object> chunkRow = jdbcTemplate.queryForMap(
                "select chunk_id, content from rag_chunks order by chunk_id limit 1");

        String chunkId = chunkRow.get("chunk_id").toString();
        String content = chunkRow.get("content").toString();

        assertThat(content).isNotBlank();

        String normalizedContent = content.trim();
        String queryText = normalizedContent.substring(0, Math.min(normalizedContent.length(), 160));
        String expectedSnippet = queryText.substring(0, Math.min(queryText.length(), 60));

        Document ragChunkDocument = new Document(chunkId, normalizedContent, Map.of("table", "rag_chunks"));

        BDDMockito.given(vectorStore.similaritySearch(BDDMockito.any(SearchRequest.class)))
                .willReturn(List.of(ragChunkDocument));

        List<Document> retrievedDocuments = retrievalService.retrieve(queryText, 5);

        assertThat(retrievedDocuments)
                .as("Expecting RetrievalService to return stubbed rag_chunks content for chunk %s", chunkId)
                .isNotEmpty()
                .anySatisfy(document -> assertThat(document.getFormattedContent()).contains(expectedSnippet));

        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        BDDMockito.then(vectorStore).should().similaritySearch(searchRequestCaptor.capture());
        SearchRequest actualSearchRequest = searchRequestCaptor.getValue();

        assertThat(actualSearchRequest.getQuery()).isEqualTo(queryText);
        assertThat(actualSearchRequest.getTopK()).isEqualTo(5);
    }
}
