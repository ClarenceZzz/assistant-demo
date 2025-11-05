package com.example.springaialibaba.preprocessor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryPreprocessorTest {

    private QueryPreprocessor queryPreprocessor;

    @BeforeEach
    void setUp() {
        queryPreprocessor = new QueryPreprocessor();
    }

    @Test
    void shouldTrimAndLowercaseInput() {
        String processed = queryPreprocessor.process("   HeLLo WorLD   ");

        assertThat(processed).isEqualTo("hello world");
    }

    @Test
    void shouldRemoveSpecialCharacters() {
        String processed = queryPreprocessor.process("What is Model-Y? @#$%^&*");

        assertThat(processed).isEqualTo("what is model-y?");
    }

    @Test
    void shouldHandleCombinationOfRules() {
        String processed = queryPreprocessor.process("  请问什么是 Model-Y？@#$ ");

        assertThat(processed).isEqualTo("请问什么是 model-y？");
    }

    @Test
    void shouldReturnEmptyStringForNullOrBlankInput() {
        assertThat(queryPreprocessor.process(null)).isEmpty();
        assertThat(queryPreprocessor.process("   ")).isEmpty();
    }

    @Test
    void shouldReplacePlaceholdersWithStandardTerms() {
        String processed = queryPreprocessor.process("Tell me about the MODEL Y features");

        assertThat(processed).isEqualTo("tell me about the model-y features");
    }
}
