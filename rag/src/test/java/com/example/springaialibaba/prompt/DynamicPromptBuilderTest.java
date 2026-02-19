package com.example.springaialibaba.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.core.io.DefaultResourceLoader;

class DynamicPromptBuilderTest {

    private PromptProperties properties;

    private DynamicPromptBuilder builder;

    private DefaultResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        resourceLoader = new DefaultResourceLoader();
        properties = new PromptProperties();
        properties.setTemplate("Persona: {persona}\nChannel: {channel}\nQuestion: {question}\nContext:\n{context}");
        properties.getDefaults().setPersona("Default Persona");
        properties.getDefaults().setChannel("Default Channel");
        builder = new DynamicPromptBuilder(properties, resourceLoader);
    }

    @Test
    void testPromptGenerationWithAllInputs() {
        List<Document> documents = List.of(new Document("Doc A"), new Document("Doc B"));

        Prompt prompt = builder.build("What is AI?", documents, "Expert", "Web");

        assertThat(prompt.getContents()).isEqualTo(
                "Persona: Expert\nChannel: Web\nQuestion: What is AI?\nContext:\nDoc A\n---\nDoc B");
    }

    @Test
    void testContextFormatting() {
        List<Document> documents = List.of(new Document("First"), new Document(""), new Document("Second"));

        Prompt prompt = builder.build("Question", documents, "Persona", "Channel");

        assertThat(prompt.getContents()).contains("First\n---\nSecond");
    }

    @Test
    void testDefaultPersonaUsage() {
        Prompt prompt = builder.build("Question", List.of(), null, "  ");

        assertThat(prompt.getContents()).contains("Persona: Default Persona");
        assertThat(prompt.getContents()).contains("Channel: Default Channel");
    }

    @Test
    void testTemplateChange() {
        PromptProperties newProperties = new PromptProperties();
        newProperties.setTemplate("Q={question}|P={persona}|C={channel}|CTX={context}");
        newProperties.getDefaults().setPersona("PersonaX");
        newProperties.getDefaults().setChannel("ChannelX");
        DynamicPromptBuilder newBuilder = new DynamicPromptBuilder(newProperties, resourceLoader);

        Prompt prompt = newBuilder.build("42?", List.of(new Document("answer")), "Guru", "SMS");

        assertThat(prompt.getContents()).isEqualTo("Q=42?|P=Guru|C=SMS|CTX=answer");
    }

    @Test
    void testLoadTemplateFromClasspathResource() {
        PromptProperties resourceProperties = new PromptProperties();
        resourceProperties.setTemplate("classpath:prompts/dynamic_prompt_template.txt");
        resourceProperties.getDefaults().setPersona("默认角色");
        resourceProperties.getDefaults().setChannel("默认渠道");
        DynamicPromptBuilder resourceBuilder = new DynamicPromptBuilder(resourceProperties, resourceLoader);

        Prompt prompt = resourceBuilder.build("问题?", List.of(new Document("上下文")), null, null);

        assertThat(prompt.getContents()).contains("你现在扮演");
        assertThat(prompt.getContents()).contains("上下文");
        assertThat(prompt.getContents()).contains("默认角色");
    }
}
