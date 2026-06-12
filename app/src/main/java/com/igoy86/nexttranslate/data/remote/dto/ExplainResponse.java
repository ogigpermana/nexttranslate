package com.igoy86.nexttranslate.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing the JSON response received from the
 * NextTranslate backend {@code POST /api/explain} endpoint.
 *
 * <p>This class belongs to the data layer and is deserialized from JSON
 * by Gson via Retrofit after a successful network response.</p>
 *
 * <p>Example JSON parsed:</p>
 * <pre>
 * {
 *   "success": true,
 *   "data": {
 *     "word": "serendipity",
 *     "language": "English",
 *     "explanation": "Serendipity refers to the pleasant surprise of...",
 *     "explanationId": "Serendipity merujuk pada kejutan menyenangkan..."
 *   }
 * }
 * </pre>
 */
public class ExplainResponse {

    /**
     * Indicates whether the explain request succeeded on the backend.
     * When {@code false}, check {@link #error} for the reason.
     */
    @SerializedName("success")
    private boolean success;

    /**
     * The explanation payload returned when {@link #success} is {@code true}.
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
     * Returns whether the explain request was successful.
     *
     * @return {@code true} if the backend processed the request successfully
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the explanation data payload.
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
     * {@link ExplainResponse}.
     */
    public static class Data {

        /** The word that was explained, echoed back from the backend. */
        @NonNull
        @SerializedName("word")
        private String word;

        /** The language of the word, echoed back from the backend. */
        @NonNull
        @SerializedName("language")
        private String language;

        /** The AI-generated explanation in English, produced by the Groq LLM. */
        @NonNull
        @SerializedName("explanation")
        private String explanation;

        /**
         * The AI-generated explanation in Indonesian (Bahasa Indonesia).
         * May be null if the backend could not produce an Indonesian version.
         */
        @Nullable
        @SerializedName("explanationId")
        private String explanationId;

        /**
         * Returns the word that was explained.
         *
         * @return the word; never null
         */
        @NonNull
        public String getWord() {
            return word;
        }

        /**
         * Returns the language of the explained word.
         *
         * @return the language name; never null
         */
        @NonNull
        public String getLanguage() {
            return language;
        }

        /**
         * Returns the AI-generated explanation in English.
         *
         * @return the English explanation; never null
         */
        @NonNull
        public String getExplanation() {
            return explanation;
        }

        /**
         * Returns the AI-generated explanation in Indonesian (Bahasa Indonesia).
         *
         * @return the Indonesian explanation, or {@code null} if not available
         */
        @Nullable
        public String getExplanationId() {
            return explanationId;
        }
    }
}
