package com.example.springaialibaba.controller;

import java.time.OffsetDateTime;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.springaialibaba.chat.generic.GenericChatApiException;
import com.example.springaialibaba.controller.dto.ApiErrorResponse;
import com.example.springaialibaba.embedding.siliconflow.SiliconFlowApiException;
import com.example.springaialibaba.rerank.siliconflow.SiliconFlowRerankException;

/**
 * Provides consistent error responses for all REST endpoints.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler({
            SiliconFlowApiException.class,
            SiliconFlowRerankException.class,
            GenericChatApiException.class
    })
    public ResponseEntity<ApiErrorResponse> handleExternalServiceExceptions(RuntimeException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, Exception ex,
            HttpServletRequest request) {
        log.error("处理请求时发生异常: {}", ex.getMessage(), ex);
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request != null ? request.getRequestURI() : null);
        return ResponseEntity.status(status).body(response);
    }
}
