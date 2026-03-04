package com.example.springaialibaba.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.example.springaialibaba.generation.GenerationService;

import org.springframework.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * 根据用户问题、上下文以及渠道、Persona 构建最终提交给大模型的 Prompt。
 */
@Service
public class DynamicPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(DynamicPromptBuilder.class);

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
        String resolvedTone = "保持礼貌但不过度热情，确保信息的传递效率是第一优先级的";
        if (resolvedChannel.equals("生活闲聊")) {
            resolvedTone = "轻松活泼，可以使用适量的表情符号和口语化的语气助词";
        }
        if (resolvedChannel.equals("售后服务")) {
            resolvedTone = "普通咨询类问题，语气要干脆利落、热心引导。不需要道歉，只需要清晰地告知步骤。投诉/故障/报修类问题，使用温和且高度专业的语言，优先承认用户遇到的困难（共情），严禁推卸责任";
        }

        String promptText = promptTemplate.replace("{persona}", resolvedPersona)
                .replace("{channel}", resolvedChannel)
                .replace("{question}", resolvedQuestion)
                .replace("{context}", resolvedContext)
                .replace("{tone}", resolvedTone);
        log.info("Prompt: {}", promptText);

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
