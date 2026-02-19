package com.example.springaialibaba.embedding.siliconflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
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

import com.example.springaialibaba.embedding.siliconflow.model.SiliconFlowEmbeddingRequest;
import com.example.springaialibaba.embedding.siliconflow.model.SiliconFlowEmbeddingResponse;
import com.example.springaialibaba.embedding.siliconflow.model.SiliconFlowEmbeddingResponse.EmbeddingData;
import com.example.springaialibaba.embedding.siliconflow.model.SiliconFlowEmbeddingResponse.Usage;

/**
 * SiliconFlow Embedding API 的 Spring AI 适配器。
 */
@Service
public class SiliconFlowEmbeddingClient implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(SiliconFlowEmbeddingClient.class);

    private final RestTemplate restTemplate;

    private final SiliconFlowEmbeddingProperties properties;

    public SiliconFlowEmbeddingClient(RestTemplate restTemplate, SiliconFlowEmbeddingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Assert.notNull(request, "EmbeddingRequest 不能为空");
        List<String> inputs = request.getInstructions();
        Assert.notEmpty(inputs, "EmbeddingRequest.instructions 不能为空");
        SiliconFlowEmbeddingRequest payload = buildPayload(inputs, request.getOptions());
        ResponseEntity<SiliconFlowEmbeddingResponse> response = invokeApi(payload);
        SiliconFlowEmbeddingResponse body = response.getBody();
        if (body == null) {
            throw new SiliconFlowApiException("SiliconFlow 响应体为空", response.getStatusCode(), null, null);
        }
        List<Embedding> embeddings = mapEmbeddings(body.getData());
        EmbeddingResponseMetadata metadata = buildMetadata(body);
        return new EmbeddingResponse(embeddings, metadata);
    }

    @Override
    public float[] embed(Document document) {
        Assert.notNull(document, "Document 不能为空");
        return firstVector(call(new EmbeddingRequest(Collections.singletonList(document.getFormattedContent()), null)));
    }

    @Override
    public float[] embed(String text) {
        Assert.hasText(text, "文本内容不能为空");
        return firstVector(call(new EmbeddingRequest(Collections.singletonList(text), null)));
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Assert.notEmpty(texts, "文本列表不能为空");
        EmbeddingResponse response = call(new EmbeddingRequest(texts, null));
        List<float[]> results = new ArrayList<>(response.getResults().size());
        for (Embedding embedding : response.getResults()) {
            results.add(embedding.getOutput());
        }
        return results;
    }

    @Override
    public int dimensions() {
        Integer dimensions = properties.getDimensions();
        return dimensions != null ? dimensions : EmbeddingModel.super.dimensions();
    }

    private float[] firstVector(EmbeddingResponse response) {
        if (response.getResults().isEmpty()) {
            return new float[0];
        }
        return response.getResults().get(0).getOutput();
    }

    private SiliconFlowEmbeddingRequest buildPayload(List<String> inputs, EmbeddingOptions options) {
        SiliconFlowEmbeddingRequest payload = new SiliconFlowEmbeddingRequest();
        payload.setModel(options != null && StringUtils.hasText(options.getModel()) ? options.getModel() : properties.getModel());
        payload.setInput(inputs.size() == 1 ? inputs.get(0) : inputs);
        Integer dims = options != null && options.getDimensions() != null ? options.getDimensions() : properties.getDimensions();
        payload.setDimensions(dims);
        payload.setEncodingFormat(properties.getEncodingFormat());
        return payload;
    }

    private ResponseEntity<SiliconFlowEmbeddingResponse> invokeApi(SiliconFlowEmbeddingRequest payload) {
        if (!StringUtils.hasText(properties.getApiUrl())) {
            throw new IllegalStateException("spring.ai.siliconflow.api-url 未配置");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("spring.ai.siliconflow.api-key 未配置");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        HttpEntity<SiliconFlowEmbeddingRequest> entity = new HttpEntity<>(payload, headers);
        try {
            return restTemplate.postForEntity(properties.getApiUrl(), entity, SiliconFlowEmbeddingResponse.class);
        }
        catch (RestClientResponseException ex) {
            log.error("调用 SiliconFlow Embedding API 失败，状态码 {}，响应体 {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new SiliconFlowApiException("调用 SiliconFlow Embedding API 失败", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        }
        catch (RestClientException ex) {
            log.error("调用 SiliconFlow Embedding API 出现异常", ex);
            throw new SiliconFlowApiException("调用 SiliconFlow Embedding API 出现异常", ex);
        }
    }

    private List<Embedding> mapEmbeddings(List<EmbeddingData> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }
        List<Embedding> embeddings = new ArrayList<>(data.size());
        for (EmbeddingData item : data) {
            float[] vector = toFloatArray(item.getEmbedding());
            embeddings.add(new Embedding(vector, item.getIndex()));
        }
        return embeddings;
    }

    private EmbeddingResponseMetadata buildMetadata(SiliconFlowEmbeddingResponse body) {
        EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
        metadata.setModel(body.getModel());
        Usage usage = body.getUsage();
        if (usage != null) {
            metadata.setUsage(new DefaultUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens(), usage));
        }
        return metadata;
    }

    private float[] toFloatArray(List<Double> source) {
        if (CollectionUtils.isEmpty(source)) {
            return new float[0];
        }
        float[] target = new float[source.size()];
        for (int i = 0; i < source.size(); i++) {
            Double value = source.get(i);
            target[i] = value != null ? value.floatValue() : 0.0f;
        }
        return target;
    }
}
