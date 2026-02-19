package com.example.springaialibaba.embedding.siliconflow;

import org.springframework.http.HttpStatusCode;

/**
 * 包装 SiliconFlow 接口调用异常。
 */
public class SiliconFlowApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    private final String responseBody;

    public SiliconFlowApiException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public SiliconFlowApiException(String message, Throwable cause) {
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
