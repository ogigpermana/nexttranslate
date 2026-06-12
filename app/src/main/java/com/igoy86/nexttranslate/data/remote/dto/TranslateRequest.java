package com.igoy86.nexttranslate.data.remote.dto;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing the request body sent to the
 * NextTranslate backend {@code POST /api/translate} endpoint.
 *
 * <p>This class belongs to the data layer and is serialized to JSON
 * by Gson via Retrofit before being sent over the network.</p>
 *
 * <p>Example JSON produced:</p>
 * <pre>
 * {
 *   "text": "Halo dunia",
 *   "sourceLang": "Indonesian",
 *   "targetLang": "English"
 * }
 * </pre>
 */
public class TranslateRequest {

    /**
     * The source text to be translated.
     * Must not be null or empty. Maximum 5000 characters.
     */
    @NonNull
    @SerializedName("text")
    private final String text;

    /**
     * The full name of the source language (e.g. "Indonesian", "English").
     * Used by the LLM prompt on the backend for better accuracy than BCP-47 codes.
     */
    @NonNull
    @SerializedName("sourceLang")
    private final String sourceLang;

    /**
     * The full name of the target language (e.g. "Japanese", "Spanish").
     * Used by the LLM prompt on the backend for better accuracy than BCP-47 codes.
     */
    @NonNull
    @SerializedName("targetLang")
    private final String targetLang;

    /**
     * Constructs a new {@link TranslateRequest}.
     *
     * @param text       the source text to translate; must not be null
     * @param sourceLang the full name of the source language; must not be null
     * @param targetLang the full name of the target language; must not be null
     */
    public TranslateRequest(
            @NonNull String text,
            @NonNull String sourceLang,
            @NonNull String targetLang
    ) {
        this.text = text;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
    }

    /**
     * Returns the source text to be translated.
     *
     * @return the source text; never null
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Returns the full name of the source language.
     *
     * @return the source language name; never null
     */
    @NonNull
    public String getSourceLang() {
        return sourceLang;
    }

    /**
     * Returns the full name of the target language.
     *
     * @return the target language name; never null
     */
    @NonNull
    public String getTargetLang() {
        return targetLang;
    }
}
