package com.igoy86.nexttranslate.domain.model;

/**
 * Domain model representing a supported translation language in the NextTranslate app.
 *
 * <p>This is a pure Java model class with no Android or framework dependencies,
 * keeping the domain layer clean and testable.</p>
 *
 * <p>Each {@link LanguageModel} holds the metadata for a single language
 * supported by ML Kit Translate, including its BCP-47 language code,
 * display name, and download status.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     LanguageModel english = new LanguageModel("en", "English", true);
 *     LanguageModel indonesian = new LanguageModel("id", "Indonesian", false);
 * </pre>
 */
public class LanguageModel {

    /**
     * The BCP-47 language code used by ML Kit Translate.
     * Examples: "en" for English, "id" for Indonesian, "zh" for Chinese.
     */
    private final String languageCode;

    /**
     * The human-readable display name of the language.
     * Examples: "English", "Indonesian", "Arabic".
     */
    private final String displayName;

    /**
     * Indicates whether the ML Kit translation model for this language
     * has already been downloaded to the device.
     *
     * <p>{@code true} if the model is available offline;
     * {@code false} if a download is required before translation.</p>
     */
    private final boolean isDownloaded;

    /**
     * Indicates whether this language is a built-in default that cannot
     * be deleted. English ("en") is the only default language in ML Kit.
     *
     * <p>{@code true} for English; {@code false} for all other languages.</p>
     */
    private final boolean isDefault;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link LanguageModel} with the given properties.
     *
     * @param languageCode the BCP-47 language code (e.g. "en", "id", "ar")
     * @param displayName  the human-readable language name (e.g. "English")
     * @param isDownloaded {@code true} if the ML Kit model is already downloaded
     * @param isDefault    {@code true} if this is a built-in language that cannot be deleted
     */
    public LanguageModel(String languageCode, String displayName, boolean isDownloaded, boolean isDefault) {
        this.languageCode = languageCode;
        this.displayName = displayName;
        this.isDownloaded = isDownloaded;
        this.isDefault = isDefault;
    }

    /**
     * Convenience constructor for non-default languages.
     *
     * @param languageCode the BCP-47 language code
     * @param displayName  the human-readable language name
     * @param isDownloaded {@code true} if the ML Kit model is already downloaded
     */
    public LanguageModel(String languageCode, String displayName, boolean isDownloaded) {
        this(languageCode, displayName, isDownloaded, false);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the BCP-47 language code for this language.
     *
     * @return the language code (e.g. "en", "id", "ar")
     */
    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Returns the human-readable display name for this language.
     *
     * @return the display name (e.g. "English", "Indonesian")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns whether the ML Kit translation model for this language
     * is already downloaded on the device.
     *
     * @return {@code true} if the model is downloaded and available offline
     */
    public boolean isDownloaded() {
        return isDownloaded;
    }

    /**
     * Returns whether this language is a built-in default that cannot be deleted.
     * English ("en") is the only built-in default in ML Kit Translate.
     *
     * @return {@code true} if this is a default language (e.g. English)
     */
    public boolean isDefault() {
        return isDefault;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a string representation of this {@link LanguageModel}
     * useful for debugging and logging.
     *
     * @return a formatted string with language code, display name, and download status
     */
    @Override
    public String toString() {
        return "LanguageModel{" +
                "languageCode='" + languageCode + '\'' +
                ", displayName='" + displayName + '\'' +
                ", isDownloaded=" + isDownloaded +
                ", isDefault=" + isDefault +
                '}';
    }

    /**
     * Checks equality based on {@link #languageCode} only, since language codes
     * are unique identifiers for each language.
     *
     * @param o the object to compare with
     * @return {@code true} if both objects have the same language code
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageModel that = (LanguageModel) o;
        return languageCode.equals(that.languageCode);
    }

    /**
     * Returns a hash code based on {@link #languageCode}.
     *
     * @return hash code derived from the language code
     */
    @Override
    public int hashCode() {
        return languageCode.hashCode();
    }
}
