package com.example.springaialibaba.chat.generic;

import org.springframework.http.HttpStatusCode;

/**
 * 包装通用 Chat API 异常。
 */
public class GenericChatApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    private final String responseBody;

    public GenericChatApiException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public GenericChatApiException(String message, Throwable cause) {
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
