package com.example.springaialibaba.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import com.example.springaialibaba.chat.generic.GenericChatApiException;
import com.example.springaialibaba.chat.generic.GenericChatClient;
import com.example.springaialibaba.prompt.DynamicPromptBuilder;

@ExtendWith(MockitoExtension.class)
class GenerationServiceTest {

    private static final String QUESTION = "who are you";

    private static final String PERSONA = "tester";

    private static final String CHANNEL = "web";

    private static final String NO_CONTEXT_FALLBACK = "no context fallback";

    private static final String ERROR_FALLBACK = "error fallback";

    @Mock
    private GenericChatClient chatClient;

    @Mock
    private DynamicPromptBuilder promptBuilder;

    private GenerationService generationService;

    @BeforeEach
    void setUp() {
        generationService = new GenerationService(chatClient, promptBuilder, NO_CONTEXT_FALLBACK, ERROR_FALLBACK, 1);
    }

    @Test
    void shouldReturnFallbackWhenContextInsufficient() {
        String answer = generationService.generate(QUESTION, Collections.emptyList(), PERSONA, CHANNEL);

        assertThat(answer).isEqualTo(NO_CONTEXT_FALLBACK);
        verifyNoInteractions(promptBuilder, chatClient);
    }

    @Test
    void shouldInvokeLlmWhenContextSufficient() {
        List<Document> context = List.of(new Document("doc-1"));
        Prompt prompt = new Prompt("prompt text");
        when(promptBuilder.build(QUESTION, context, PERSONA, CHANNEL)).thenReturn(prompt);
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage("llm answer"))));
        when(chatClient.call(prompt)).thenReturn(response);

        String answer = generationService.generate(QUESTION, context, PERSONA, CHANNEL);

        assertThat(answer).isEqualTo("llm answer");
        verify(promptBuilder).build(QUESTION, context, PERSONA, CHANNEL);
        verify(chatClient).call(prompt);
    }

    @Test
    void shouldReturnErrorFallbackWhenLlmFails() {
        List<Document> context = List.of(new Document("doc-1"));
        Prompt prompt = new Prompt("prompt text");
        when(promptBuilder.build(QUESTION, context, PERSONA, CHANNEL)).thenReturn(prompt);
        when(chatClient.call(prompt)).thenThrow(new GenericChatApiException("call failed", new RuntimeException("boom")));

        String answer = generationService.generate(QUESTION, context, PERSONA, CHANNEL);

        assertThat(answer).isEqualTo(ERROR_FALLBACK);
        verify(promptBuilder).build(QUESTION, context, PERSONA, CHANNEL);
        verify(chatClient).call(prompt);
    }
}
