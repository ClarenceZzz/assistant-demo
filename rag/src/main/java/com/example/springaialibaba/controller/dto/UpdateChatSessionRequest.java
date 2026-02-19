package com.example.springaialibaba.controller.dto;

/**
 * Request payload for updating chat session metadata.
 */
public record UpdateChatSessionRequest(String title, String category) {
}
