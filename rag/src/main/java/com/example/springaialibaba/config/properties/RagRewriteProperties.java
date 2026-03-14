package com.example.springaialibaba.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Query rewrite 相关配置。
 */
@ConfigurationProperties(prefix = "app.rag.rewrite")
public class RagRewriteProperties {

    /**
     * 是否启用 LLM 查询改写。
     */
    private boolean enabled = true;

    /**
     * 改写 Prompt 模板，可以是资源路径或内联模板。
     */
    private String promptTemplate = "classpath:prompts/rag_rewrite_prompt.txt";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }
}
