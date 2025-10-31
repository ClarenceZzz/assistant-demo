package com.example.springaialibaba.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    public JdbcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        return List.of(
                new PgObjectToJsonNodeConverter(objectMapper),
                new JsonNodeToStringConverter(objectMapper),
                new StringToJsonNodeConverter(objectMapper));
    }

    @ReadingConverter
    static class PgObjectToJsonNodeConverter implements Converter<PGobject, JsonNode> {

        private final ObjectMapper objectMapper;

        PgObjectToJsonNodeConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode convert(PGobject source) {
            if (source == null || source.getValue() == null) {
                return null;
            }
            try {
                return objectMapper.readTree(source.getValue());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to deserialize JSONB to JsonNode", e);
            }
        }
    }

    @WritingConverter
    static class JsonNodeToStringConverter implements Converter<JsonNode, String> {

        private final ObjectMapper objectMapper;

        JsonNodeToStringConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public String convert(JsonNode source) {
            if (source == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize JsonNode to String", e);
            }
        }
    }

    @ReadingConverter
    static class StringToJsonNodeConverter implements Converter<String, JsonNode> {

        private final ObjectMapper objectMapper;

        StringToJsonNodeConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode convert(String source) {
            if (source == null) {
                return null;
            }
            try {
                return objectMapper.readTree(source);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to deserialize String to JsonNode", e);
            }
        }
    }
}
