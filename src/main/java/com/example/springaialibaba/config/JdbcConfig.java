package com.example.springaialibaba.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
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
                new JsonNodeToPgObjectConverter(objectMapper),
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
    static class JsonNodeToPgObjectConverter implements Converter<JsonNode, PGobject> {

        private final ObjectMapper objectMapper;

        JsonNodeToPgObjectConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public PGobject convert(JsonNode source) {
            if (source == null) {
                return null;
            }
            try {
                PGobject pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(objectMapper.writeValueAsString(source));
                return pgObject;
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize JsonNode to JSONB", e);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Failed to wrap JsonNode as PGobject", e);
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
