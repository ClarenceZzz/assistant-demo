package com.example.springaialibaba.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * 根据用户问题、上下文以及渠道、Persona 构建最终提交给大模型的 Prompt。
 */
@Service
public class DynamicPromptBuilder {

    private final PromptProperties properties;

    private final ResourceLoader resourceLoader;

    private final String promptTemplate;

    public DynamicPromptBuilder(PromptProperties properties, ResourceLoader resourceLoader) {
        Assert.notNull(properties, "PromptProperties must not be null");
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.promptTemplate = loadTemplate(properties.getTemplate());
    }

    /**
     * 构建动态 Prompt。
     *
     * @param question 用户问题
     * @param context 经过重排序后的上下文文档
     * @param persona 用户 Persona
     * @param channel 渠道信息
     * @return 包含完整文本的 Prompt 对象
     */
    public Prompt build(String question, List<Document> context, String persona, String channel) {
        String resolvedPersona = resolveFallback(persona, properties.getDefaults().getPersona());
        String resolvedChannel = resolveFallback(channel, properties.getDefaults().getChannel());
        String resolvedQuestion = Objects.toString(question, "");
        String resolvedContext = formatContext(context);

        String promptText = promptTemplate.replace("{persona}", resolvedPersona)
                .replace("{channel}", resolvedChannel)
                .replace("{question}", resolvedQuestion)
                .replace("{context}", resolvedContext);

        return new Prompt(promptText);
    }

    private String formatContext(List<Document> context) {
        if (CollectionUtils.isEmpty(context)) {
            return "";
        }

        return context.stream()
                .filter(Objects::nonNull)
                .map(Document::getText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n---\n"));
    }

    private String loadTemplate(String templateConfig) {
        if (!StringUtils.hasText(templateConfig)) {
            throw new IllegalStateException("Prompt 模板未配置，请在 prompt.template 中提供模板或资源路径");
        }

        Resource resource = resourceLoader.getResource(templateConfig);
        if (resource.exists()) {
            try (InputStream inputStream = resource.getInputStream()) {
                return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
            catch (IOException ex) {
                throw new IllegalStateException("读取 Prompt 模板失败: " + templateConfig, ex);
            }
        }

        return templateConfig;
    }

    private String resolveFallback(String candidate, String fallback) {
        if (StringUtils.hasText(candidate)) {
            return candidate;
        }
        return StringUtils.hasText(fallback) ? fallback : "";
    }
}
