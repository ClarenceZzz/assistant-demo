package com.example.springaialibaba.embedding;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 基于文本内容生成确定性向量的本地嵌入模型，避免开发阶段依赖外部嵌入服务。
 */
@Component
@Primary
public class DeterministicEmbeddingModel implements EmbeddingModel {

    public static final int VECTOR_SIZE = 1536;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Assert.notNull(request, "EmbeddingRequest 不能为空");
        List<String> inputs = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(inputs.size());

        for (int index = 0; index < inputs.size(); index++) {
            String input = inputs.get(index);
            embeddings.add(new Embedding(generateVector(input), index));
        }

        EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
        metadata.setModel("deterministic-local");
        return new EmbeddingResponse(embeddings, metadata);
    }

    @Override
    public float[] embed(Document document) {
        Assert.notNull(document, "Document 不能为空");
        return generateVector(document.getFormattedContent());
    }

    @Override
    public int dimensions() {
        return VECTOR_SIZE;
    }

    private float[] generateVector(String input) {
        String text = (input != null) ? input : "";
        float[] vector = new float[VECTOR_SIZE];
        if (text.isEmpty()) {
            return vector;
        }
        char[] chars = text.toCharArray();
        for (char ch : chars) {
            int index = Math.abs(ch) % VECTOR_SIZE;
            vector[index] += 1.0f;
        }
        float norm = (float) Math.sqrt(chars.length);
        if (norm > 0) {
            for (int i = 0; i < VECTOR_SIZE; i++) {
                vector[i] = vector[i] / norm;
            }
        }
        return vector;
    }
}
