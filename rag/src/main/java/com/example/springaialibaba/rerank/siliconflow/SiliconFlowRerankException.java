package com.example.springaialibaba.rerank.siliconflow;

import org.springframework.http.HttpStatusCode;

/**
 * 表示调用 SiliconFlow Rerank API 发生的异常。
 */
public class SiliconFlowRerankException extends RuntimeException {

    private final HttpStatusCode statusCode;

    private final String responseBody;

    public SiliconFlowRerankException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public SiliconFlowRerankException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.responseBody = null;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
