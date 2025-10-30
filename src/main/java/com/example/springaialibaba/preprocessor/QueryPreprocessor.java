package com.example.springaialibaba.preprocessor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Service responsible for normalising raw user queries before they are sent to the retrieval
 * pipeline. The normalisation process strips noisy characters, unifies casing and applies a
 * curated set of placeholder replacements to align the query with internal terminology.
 */
@Service
public class QueryPreprocessor {

    private static final String ALLOWED_PUNCTUATION =
            " .,!?:;()[]{}<>-_'\"“”‘’·，。？！：；、（）《》【】…";
    private static final Pattern DISALLOWED_CHARACTERS = Pattern.compile(
            "[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}\\s" + Pattern.quote(ALLOWED_PUNCTUATION) + "]");
    private static final Pattern MULTIPLE_WHITESPACES = Pattern.compile("\\s+");

    private final Map<String, String> placeholderReplacements;

    public QueryPreprocessor() {
        this(createDefaultReplacements());
    }

    QueryPreprocessor(Map<String, String> placeholderReplacements) {
        this.placeholderReplacements = placeholderReplacements;
    }

    /**
     * Process the raw user query and return a cleaned, normalised version ready for downstream
     * consumption.
     *
     * @param rawQuery the raw query from end users; may be {@code null}
     * @return a cleaned query string, or an empty string when the input is {@code null} or blank
     */
    public String process(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }

        String normalised = rawQuery.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            return "";
        }

        normalised = DISALLOWED_CHARACTERS.matcher(normalised).replaceAll("");
        normalised = MULTIPLE_WHITESPACES.matcher(normalised).replaceAll(" ");
        normalised = applyPlaceholderReplacements(normalised);
        return normalised.trim();
    }

    private String applyPlaceholderReplacements(String text) {
        String current = text;
        for (Map.Entry<String, String> entry : placeholderReplacements.entrySet()) {
            current = current.replace(entry.getKey(), entry.getValue());
        }
        return current;
    }

    private static Map<String, String> createDefaultReplacements() {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("model y", "model-y");
        return Collections.unmodifiableMap(replacements);
    }
}
