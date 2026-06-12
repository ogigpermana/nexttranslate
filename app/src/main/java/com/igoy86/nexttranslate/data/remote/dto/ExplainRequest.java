package com.igoy86.nexttranslate.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing the request body sent to the
 * NextTranslate backend {@code POST /api/explain} endpoint.
 *
 * <p>This class belongs to the data layer and is serialized to JSON
 * by Gson via Retrofit before being sent over the network.</p>
 *
 * <p>Example JSON produced:</p>
 * <pre>
 * {
 *   "word": "serendipity",
 *   "language": "English",
 *   "definition": "the occurrence of events by chance in a happy way"
 * }
 * </pre>
 *
 * <p>The {@code definition} field is optional — the backend will still
 * produce a rich explanation without it, but including it improves
 * the contextual accuracy of the AI response.</p>
 */
public class ExplainRequest {

    /**
     * The word or short phrase to explain.
     * Must not be null or empty. Maximum 200 characters (enforced by backend).
     */
    @NonNull
    @SerializedName("word")
    private final String word;

    /**
     * The full English name of the language the word belongs to
     * (e.g. {@code "English"}, {@code "Japanese"}, {@code "Indonesian"}).
     * Used by the LLM to tailor the explanation.
     */
    @NonNull
    @SerializedName("language")
    private final String language;

    /**
     * An optional short definition providing context for the AI explanation.
     * When provided, the backend uses it as a hint so the LLM focuses on
     * the correct meaning rather than guessing.
     */
    @Nullable
    @SerializedName("definition")
    private final String definition;

    /**
     * Constructs a new {@link ExplainRequest} with all fields.
     *
     * @param word       the word to explain; must not be null or empty
     * @param language   the full English name of the word's language; must not be null
     * @param definition an optional short definition for context; may be null
     */
    public ExplainRequest(
            @NonNull String word,
            @NonNull String language,
            @Nullable String definition
    ) {
        this.word = word;
        this.language = language;
        this.definition = definition;
    }

    /**
     * Returns the word to explain.
     *
     * @return the word; never null
     */
    @NonNull
    public String getWord() {
        return word;
    }

    /**
     * Returns the language name of the word.
     *
     * @return the language name; never null
     */
    @NonNull
    public String getLanguage() {
        return language;
    }

    /**
     * Returns the optional definition hint.
     *
     * @return the definition, or {@code null} if not provided
     */
    @Nullable
    public String getDefinition() {
        return definition;
    }
}
