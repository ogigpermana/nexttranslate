package com.igoy86.nexttranslate.domain.model;

/**
 * Domain model representing the result of a translation operation
 * performed by ML Kit Translate.
 *
 * <p>This is a pure Java model class with no Android or framework dependencies,
 * keeping the domain layer clean and testable.</p>
 *
 * <p>A {@link TranslationResult} encapsulates all data produced after a
 * successful translation, including the original text, the translated text,
 * the detected or selected source language, and the target language.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     TranslationResult result = new TranslationResult(
 *         "Hello, world!",
 *         "Halo, dunia!",
 *         "en",
 *         "id",
 *         System.currentTimeMillis()
 *     );
 * </pre>
 */
public class TranslationResult {

    /**
     * The original text submitted for translation.
     */
    private final String sourceText;

    /**
     * The translated output text produced by ML Kit Translate.
     */
    private final String translatedText;

    /**
     * The BCP-47 language code of the source language.
     *
     * <p>This may be the language selected manually by the user,
     * or the language detected automatically by ML Kit Language ID.</p>
     */
    private final String sourceLanguageCode;

    /**
     * The BCP-47 language code of the target language selected by the user.
     */
    private final String targetLanguageCode;

    /**
     * The Unix timestamp (in milliseconds) when this translation was produced.
     * Used for sorting and display in the history screen.
     */
    private final long timestamp;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link TranslationResult} with all required fields.
     *
     * @param sourceText          the original text that was translated
     * @param translatedText      the output text after translation
     * @param sourceLanguageCode  the BCP-47 code of the source language (e.g. "en")
     * @param targetLanguageCode  the BCP-47 code of the target language (e.g. "id")
     * @param timestamp           the Unix timestamp in milliseconds when the
     *                            translation was performed
     */
    public TranslationResult(
            String sourceText,
            String translatedText,
            String sourceLanguageCode,
            String targetLanguageCode,
            long timestamp
    ) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.sourceLanguageCode = sourceLanguageCode;
        this.targetLanguageCode = targetLanguageCode;
        this.timestamp = timestamp;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the original text that was submitted for translation.
     *
     * @return the source text string
     */
    public String getSourceText() {
        return sourceText;
    }

    /**
     * Returns the translated output text produced by ML Kit Translate.
     *
     * @return the translated text string
     */
    public String getTranslatedText() {
        return translatedText;
    }

    /**
     * Returns the BCP-47 language code of the source language.
     *
     * @return the source language code (e.g. "en", "id", "ar")
     */
    public String getSourceLanguageCode() {
        return sourceLanguageCode;
    }

    /**
     * Returns the BCP-47 language code of the target language.
     *
     * @return the target language code (e.g. "en", "id", "ar")
     */
    public String getTargetLanguageCode() {
        return targetLanguageCode;
    }

    /**
     * Returns the Unix timestamp in milliseconds when this translation
     * was performed.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a string representation of this {@link TranslationResult}
     * useful for debugging and logging.
     *
     * @return a formatted string with all fields
     */
    @Override
    public String toString() {
        return "TranslationResult{" +
                "sourceText='" + sourceText + '\'' +
                ", translatedText='" + translatedText + '\'' +
                ", sourceLanguageCode='" + sourceLanguageCode + '\'' +
                ", targetLanguageCode='" + targetLanguageCode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Checks equality based on {@link #sourceText}, {@link #sourceLanguageCode},
     * and {@link #targetLanguageCode}, since the same text translated between
     * the same language pair should be considered equal regardless of timestamp.
     *
     * @param o the object to compare with
     * @return {@code true} if source text and language pair are identical
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranslationResult that = (TranslationResult) o;
        return sourceText.equals(that.sourceText)
                && sourceLanguageCode.equals(that.sourceLanguageCode)
                && targetLanguageCode.equals(that.targetLanguageCode);
    }

    /**
     * Returns a hash code based on source text and language pair.
     *
     * @return hash code derived from sourceText, sourceLanguageCode,
     *         and targetLanguageCode
     */
    @Override
    public int hashCode() {
        int result = sourceText.hashCode();
        result = 31 * result + sourceLanguageCode.hashCode();
        result = 31 * result + targetLanguageCode.hashCode();
        return result;
    }
}