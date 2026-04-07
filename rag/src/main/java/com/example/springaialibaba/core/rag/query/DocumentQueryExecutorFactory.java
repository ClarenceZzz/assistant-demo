package com.example.springaialibaba.core.rag.query;

import com.example.springaialibaba.core.rag.routing.RouteKey;

/**
 * Hard-coded route-to-executor mapping for the current phase.
 */
public class DocumentQueryExecutorFactory {

    private final PgVectorDocumentQueryExecutor pgVectorExecutor;

    private final MysqlDocumentQueryExecutor mysqlExecutor;

    private final EsKeywordDocumentQueryExecutor esKeywordExecutor;

    public DocumentQueryExecutorFactory(PgVectorDocumentQueryExecutor pgVectorExecutor,
            MysqlDocumentQueryExecutor mysqlExecutor,
            EsKeywordDocumentQueryExecutor esKeywordExecutor) {
        this.pgVectorExecutor = pgVectorExecutor;
        this.mysqlExecutor = mysqlExecutor;
        this.esKeywordExecutor = esKeywordExecutor;
    }

    public DocumentQueryExecutor getExecutor(RouteKey routeKey) {
        if (routeKey == null) {
            return pgVectorExecutor;
        }
        return switch (routeKey) {
            case MYSQL -> mysqlExecutor;
            case ES_KEYWORD -> esKeywordExecutor;
            case PG_VECTOR -> pgVectorExecutor;
        };
    }
}
