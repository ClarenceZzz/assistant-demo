package com.example.springaialibaba.embedding.siliconflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SiliconFlow Embedding API 请求体。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiliconFlowEmbeddingRequest {

    private String model;

    private Object input;

    @JsonProperty("encoding_format")
    private String encodingFormat;

    private Integer dimensions;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public String getEncodingFormat() {
        return encodingFormat;
    }

    public void setEncodingFormat(String encodingFormat) {
        this.encodingFormat = encodingFormat;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }
}
