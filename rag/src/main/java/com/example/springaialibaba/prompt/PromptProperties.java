package com.example.springaialibaba.prompt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Prompt 生成相关的配置属性。
 */
@ConfigurationProperties(prefix = "prompt")
public class PromptProperties {

    /**
     * Prompt 模板配置，可以是包含占位符的字符串或资源位置（如 classpath:prompts/...）。
     */
    private String template = "";

    private final Defaults defaults = new Defaults();

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public static class Defaults {

        private String persona = "";

        private String channel = "";

        public String getPersona() {
            return persona;
        }

        public void setPersona(String persona) {
            this.persona = persona;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }
    }
}
