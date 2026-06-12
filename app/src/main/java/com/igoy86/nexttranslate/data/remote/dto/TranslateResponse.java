package com.igoy86.nexttranslate.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing the JSON response received from the
 * NextTranslate backend {@code POST /api/translate} endpoint.
 *
 * <p>This class belongs to the data layer and is deserialized from JSON
 * by Gson via Retrofit after a successful network response.</p>
 *
 * <p>Example JSON parsed:</p>
 * <pre>
 * {
 *   "success": true,
 *   "data": {
 *     "translatedText": "Hello world",
 *     "sourceLang": "Indonesian",
 *     "targetLang": "English",
 *     "originalText": "Halo dunia"
 *   }
 * }
 * </pre>
 */
public class TranslateResponse {

    /**
     * Indicates whether the translation request succeeded on the backend.
     * When {@code false}, check {@link #error} for the reason.
     */
    @SerializedName("success")
    private boolean success;

    /**
     * The translation payload returned when {@link #success} is {@code true}.
     * Will be {@code null} on failure.
     */
    @Nullable
    @SerializedName("data")
    private Data data;

    /**
     * Human-readable error message returned by the backend when
     * {@link #success} is {@code false}.
     */
    @Nullable
    @SerializedName("error")
    private String error;

    /**
     * Returns whether the translation request was successful.
     *
     * @return {@code true} if the backend processed the request successfully
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the translation data payload.
     *
     * @return the {@link Data} object, or {@code null} if the request failed
     */
    @Nullable
    public Data getData() {
        return data;
    }

    /**
     * Returns the backend error message, if any.
     *
     * @return the error string, or {@code null} if the request succeeded
     */
    @Nullable
    public String getError() {
        return error;
    }

    // -------------------------------------------------------------------------
    // Nested data payload
    // -------------------------------------------------------------------------

    /**
     * Nested class representing the {@code data} field inside a successful
     * {@link TranslateResponse}.
     */
    public static class Data {

        /** The translated text produced by the Groq LLM. */
        @NonNull
        @SerializedName("translatedText")
        private String translatedText;

        /** The full name of the source language sent in the request. */
        @NonNull
        @SerializedName("sourceLang")
        private String sourceLang;

        /** The full name of the target language sent in the request. */
        @NonNull
        @SerializedName("targetLang")
        private String targetLang;

        /** The original text that was submitted for translation. */
        @NonNull
        @SerializedName("originalText")
        private String originalText;

        /**
         * Returns the translated text.
         *
         * @return the translated text; never null
         */
        @NonNull
        public String getTranslatedText() {
            return translatedText;
        }

        /**
         * Returns the source language name.
         *
         * @return the source language name; never null
         */
        @NonNull
        public String getSourceLang() {
            return sourceLang;
        }

        /**
         * Returns the target language name.
         *
         * @return the target language name; never null
         */
        @NonNull
        public String getTargetLang() {
            return targetLang;
        }

        /**
         * Returns the original text that was submitted for translation.
         *
         * @return the original text; never null
         */
        @NonNull
        public String getOriginalText() {
            return originalText;
        }
    }
}
