package com.example.springaialibaba.chat.generic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.example.springaialibaba.chat.generic.model.ChatCompletionRequest;
import com.example.springaialibaba.chat.generic.model.ChatCompletionResponse;
import com.example.springaialibaba.chat.generic.model.ChatCompletionRequest.ChatCompletionMessage;

import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * 通用的 OpenAI 兼容 Chat 客户端。
 */
@Service
public class GenericChatClient implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(GenericChatClient.class);

    private final RestTemplate restTemplate;

    private final GenericChatProperties properties;

    public GenericChatClient(RestTemplate restTemplate, GenericChatProperties properties) {
        this.restTemplate = configureTimeouts(restTemplate, properties);
        this.properties = properties;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Assert.notNull(prompt, "Prompt 不能为空");
        ChatCompletionRequest request = buildRequest(prompt);
        ResponseEntity<ChatCompletionResponse> response = invokeApi(request);
        ChatCompletionResponse body = response.getBody();
        if (body == null) {
            throw new GenericChatApiException("通用 Chat API 响应体为空", response.getStatusCode(), null, null);
        }
        List<Generation> generations = mapGenerations(body.getChoices());
        return new ChatResponse(generations);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    private ChatCompletionRequest buildRequest(Prompt prompt) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        ChatOptions options = prompt.getOptions();
        request.setModel(resolveModel(options));
        request.setMessages(mapMessages(prompt));
        request.setTemperature(resolve(options != null ? options.getTemperature() : null, properties.getTemperature()));
        request.setTopP(resolve(options != null ? options.getTopP() : null, properties.getTopP()));
        request.setTopK(resolve(options != null ? options.getTopK() : null, properties.getTopK()));
        request.setMaxTokens(resolve(options != null ? options.getMaxTokens() : null, properties.getMaxTokens()));
        request.setFrequencyPenalty(resolve(options != null ? options.getFrequencyPenalty() : null, properties.getFrequencyPenalty()));
        request.setN(properties.getN());
        request.setStream(properties.getStream());
        request.setEnableThinking(properties.getEnableThinking());
        request.setThinkingBudget(properties.getThinkingBudget());
        if (options != null && !CollectionUtils.isEmpty(options.getStopSequences())) {
            request.setStop(options.getStopSequences());
        }
        return request;
    }

    private ResponseEntity<ChatCompletionResponse> invokeApi(ChatCompletionRequest request) {
        validateConfiguration();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);
        try {
            return restTemplate.postForEntity(properties.getApiUrl(), entity, ChatCompletionResponse.class);
        }
        catch (RestClientResponseException ex) {
            log.error("调用通用 Chat API 失败，状态码 {}，响应体 {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GenericChatApiException("调用通用 Chat API 失败", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        }
        catch (RestClientException ex) {
            log.error("调用通用 Chat API 出现异常", ex);
            throw new GenericChatApiException("调用通用 Chat API 出现异常", ex);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getApiUrl())) {
            throw new IllegalStateException("spring.ai.generic-chat.api-url 未配置");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("spring.ai.generic-chat.api-key 未配置");
        }
    }

    private String resolveModel(ChatOptions options) {
        if (options != null && StringUtils.hasText(options.getModel())) {
            return options.getModel();
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("spring.ai.generic-chat.model 未配置");
        }
        return properties.getModel();
    }

    private List<ChatCompletionMessage> mapMessages(Prompt prompt) {
        List<Message> instructions = prompt.getInstructions();
        if (CollectionUtils.isEmpty(instructions)) {
            String content = prompt.getContents();
            if (!StringUtils.hasText(content)) {
                throw new IllegalArgumentException("Prompt 不包含任何消息内容");
            }
            return List.of(new ChatCompletionMessage(MessageType.USER.getValue(), content));
        }
        List<ChatCompletionMessage> messages = new ArrayList<>(instructions.size());
        for (Message instruction : instructions) {
            if (instruction == null) {
                continue;
            }
            String role = resolveRole(instruction.getMessageType());
            String content = instruction.getText();
            messages.add(new ChatCompletionMessage(role, content));
        }
        return messages;
    }

    private List<Generation> mapGenerations(Collection<ChatCompletionResponse.Choice> choices) {
        if (CollectionUtils.isEmpty(choices)) {
            return Collections.singletonList(new Generation(new AssistantMessage("")));
        }
        List<Generation> generations = new ArrayList<>();
        for (ChatCompletionResponse.Choice choice : choices) {
            if (choice == null) {
                continue;
            }
            ChatCompletionResponse.Message message = choice.getMessage();
            String content = message != null ? message.getContent() : null;
            AssistantMessage assistantMessage = new AssistantMessage(content != null ? content : "");
            generations.add(new Generation(assistantMessage));
        }
        return generations;
    }

    private String resolveRole(MessageType messageType) {
        return messageType != null ? messageType.getValue() : MessageType.USER.getValue();
    }

    private <T> T resolve(T override, T defaultValue) {
        return override != null ? override : defaultValue;
    }

    private RestTemplate configureTimeouts(RestTemplate restTemplate, GenericChatProperties properties) {
        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();
        if (factory instanceof SimpleClientHttpRequestFactory simpleFactory) {
            Duration connectTimeout = Objects.requireNonNullElse(properties.getConnectTimeout(), Duration.ofSeconds(10));
            Duration readTimeout = Objects.requireNonNullElse(properties.getReadTimeout(), Duration.ofSeconds(30));
            simpleFactory.setConnectTimeout((int) connectTimeout.toMillis());
            simpleFactory.setReadTimeout((int) readTimeout.toMillis());
        }
        return restTemplate;
    }
}
