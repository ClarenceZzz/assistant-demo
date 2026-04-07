package com.example.springaialibaba.rag.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.springaialibaba.core.rag.query.DocumentQueryExecutor;
import com.example.springaialibaba.core.rag.query.DocumentQueryExecutorFactory;
import com.example.springaialibaba.core.rag.query.EsKeywordDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.query.MysqlDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.query.PgVectorDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import org.junit.jupiter.api.Test;

class DocumentQueryExecutorFactoryTest {

    @Test
    void shouldMapRouteKeyToExpectedExecutor() {
        PgVectorDocumentQueryExecutor pgExecutor = mock(PgVectorDocumentQueryExecutor.class);
        MysqlDocumentQueryExecutor mysqlExecutor = mock(MysqlDocumentQueryExecutor.class);
        EsKeywordDocumentQueryExecutor esExecutor = mock(EsKeywordDocumentQueryExecutor.class);
        DocumentQueryExecutorFactory factory = new DocumentQueryExecutorFactory(pgExecutor, mysqlExecutor, esExecutor);

        DocumentQueryExecutor mysql = factory.getExecutor(RouteKey.MYSQL);
        DocumentQueryExecutor pg = factory.getExecutor(RouteKey.PG_VECTOR);
        DocumentQueryExecutor es = factory.getExecutor(RouteKey.ES_KEYWORD);

        assertThat(mysql).isSameAs(mysqlExecutor);
        assertThat(pg).isSameAs(pgExecutor);
        assertThat(es).isSameAs(esExecutor);
        assertThat(factory.getExecutor(null)).isSameAs(pgExecutor);
    }
}
