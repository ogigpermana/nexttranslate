package com.igoy86.nexttranslate.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object representing the JSON response from the
 * Free Dictionary API {@code GET /api/v1/entries/{language}/{word}} endpoint.
 *
 * <p>This class belongs to the data layer and is deserialized from JSON
 * by Gson via Retrofit. It mirrors the {@code EntriesByLanguageAndWord}
 * schema defined in the OpenAPI 3.0 specification.</p>
 *
 * <p>Example JSON parsed:</p>
 * <pre>
 * {
 *   "word": "hello",
 *   "entries": [
 *     {
 *       "language": { "code": "en", "name": "English" },
 *       "partOfSpeech": "interjection",
 *       "pronunciations": [{ "type": "ipa", "text": "/həˈloʊ/", "tags": [] }],
 *       "forms": [],
 *       "senses": [
 *         {
 *           "definition": "A greeting.",
 *           "tags": [],
 *           "examples": ["Hello, how are you?"],
 *           "quotes": [],
 *           "synonyms": ["hi", "hey"],
 *           "antonyms": [],
 *           "subsenses": []
 *         }
 *       ],
 *       "synonyms": [],
 *       "antonyms": []
 *     }
 *   ],
 *   "source": {
 *     "url": "https://en.wiktionary.org/wiki/hello",
 *     "license": { "name": "CC BY-SA 4.0", "url": "https://..." }
 *   }
 * }
 * </pre>
 */
public class DictionaryResponseDto {

    /**
     * The word that was looked up.
     */
    @NonNull
    @SerializedName("word")
    private String word;

    /**
     * All dictionary entries for this word across different languages and
     * parts of speech.
     */
    @NonNull
    @SerializedName("entries")
    private List<EntryDto> entries;

    /**
     * Attribution and licensing information for the returned data.
     */
    @Nullable
    @SerializedName("source")
    private SourceDto source;

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** @return the word that was looked up; never null */
    @NonNull
    public String getWord() { return word; }

    /** @return list of all dictionary entries; never null */
    @NonNull
    public List<EntryDto> getEntries() { return entries; }

    /** @return source attribution info, or null if absent */
    @Nullable
    public SourceDto getSource() { return source; }

    // =========================================================================
    // Nested DTOs
    // =========================================================================

    /**
     * Represents a single dictionary entry for the word in one specific
     * language and part of speech.
     *
     * <p>Maps to the {@code Entry} schema in the OpenAPI spec.</p>
     */
    public static class EntryDto {

        /** The language this entry belongs to. */
        @Nullable
        @SerializedName("language")
        private LanguageDto language;

        /** The grammatical category, e.g. "noun", "verb", "adjective". */
        @Nullable
        @SerializedName("partOfSpeech")
        private String partOfSpeech;

        /** Phonetic pronunciations for this word. */
        @Nullable
        @SerializedName("pronunciations")
        private List<PronunciationDto> pronunciations;

        /** Inflected forms such as plural or past tense. */
        @Nullable
        @SerializedName("forms")
        private List<FormDto> forms;

        /** All meanings (senses) for this entry. */
        @Nullable
        @SerializedName("senses")
        private List<SenseDto> senses;

        /** Entry-level synonyms (across all senses). */
        @Nullable
        @SerializedName("synonyms")
        private List<String> synonyms;

        /** Entry-level antonyms (across all senses). */
        @Nullable
        @SerializedName("antonyms")
        private List<String> antonyms;

        // Getters
        @Nullable public LanguageDto getLanguage() { return language; }
        @Nullable public String getPartOfSpeech() { return partOfSpeech; }
        @Nullable public List<PronunciationDto> getPronunciations() { return pronunciations; }
        @Nullable public List<FormDto> getForms() { return forms; }
        @Nullable public List<SenseDto> getSenses() { return senses; }
        @Nullable public List<String> getSynonyms() { return synonyms; }
        @Nullable public List<String> getAntonyms() { return antonyms; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents a language identifier with an ISO code and display name.
     *
     * <p>Maps to the {@code Language} schema in the OpenAPI spec.</p>
     */
    public static class LanguageDto {

        /** ISO 639-1/639-3 language code (e.g. "en", "id"). */
        @NonNull
        @SerializedName("code")
        private String code;

        /** Full English name of the language (e.g. "English"). */
        @NonNull
        @SerializedName("name")
        private String name;

        @NonNull public String getCode() { return code; }
        @NonNull public String getName() { return name; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents a phonetic pronunciation in IPA or enPR notation.
     *
     * <p>Maps to the {@code Pronunciation} schema in the OpenAPI spec.</p>
     */
    public static class PronunciationDto {

        /**
         * The notation type: {@code "ipa"} or {@code "enpr"}.
         */
        @Nullable
        @SerializedName("type")
        private String type;

        /** The pronunciation text (e.g. "/həˈloʊ/"). */
        @Nullable
        @SerializedName("text")
        private String text;

        /** Dialect or usage tags (e.g. "US", "UK", "formal"). */
        @Nullable
        @SerializedName("tags")
        private List<String> tags;

        @Nullable public String getType() { return type; }
        @Nullable public String getText() { return text; }
        @Nullable public List<String> getTags() { return tags; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents an inflected form of the word (e.g. plural, past tense).
     *
     * <p>Maps to the {@code Form} schema in the OpenAPI spec.</p>
     */
    public static class FormDto {

        /** The inflected word form. */
        @Nullable
        @SerializedName("word")
        private String word;

        /** Labels describing the form (e.g. "plural", "past tense"). */
        @Nullable
        @SerializedName("tags")
        private List<String> tags;

        @Nullable public String getWord() { return word; }
        @Nullable public List<String> getTags() { return tags; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents one specific meaning (sense) of the word.
     *
     * <p>Maps to the {@code Sense} schema in the OpenAPI spec.
     * A sense may recursively contain {@link #subsenses}.</p>
     */
    public static class SenseDto {

        /** The definition text for this meaning. */
        @Nullable
        @SerializedName("definition")
        private String definition;

        /** Usage tags (e.g. "formal", "archaic", "technical"). */
        @Nullable
        @SerializedName("tags")
        private List<String> tags;

        /** Example sentences demonstrating this meaning. */
        @Nullable
        @SerializedName("examples")
        private List<String> examples;

        /** Literary quotes using the word in this sense. */
        @Nullable
        @SerializedName("quotes")
        private List<QuoteDto> quotes;

        /** Synonyms specific to this sense. */
        @Nullable
        @SerializedName("synonyms")
        private List<String> synonyms;

        /** Antonyms specific to this sense. */
        @Nullable
        @SerializedName("antonyms")
        private List<String> antonyms;

        /**
         * Translations of this sense into other languages.
         * Only present when the request includes {@code ?translations=true}.
         */
        @Nullable
        @SerializedName("translations")
        private List<TranslationDto> translations;

        /** More specific sub-meanings within this sense. */
        @Nullable
        @SerializedName("subsenses")
        private List<SenseDto> subsenses;

        @Nullable public String getDefinition() { return definition; }
        @Nullable public List<String> getTags() { return tags; }
        @Nullable public List<String> getExamples() { return examples; }
        @Nullable public List<QuoteDto> getQuotes() { return quotes; }
        @Nullable public List<String> getSynonyms() { return synonyms; }
        @Nullable public List<String> getAntonyms() { return antonyms; }
        @Nullable public List<TranslationDto> getTranslations() { return translations; }
        @Nullable public List<SenseDto> getSubsenses() { return subsenses; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents a literary quote showing the word used in context.
     *
     * <p>Maps to the {@code Quote} schema in the OpenAPI spec.</p>
     */
    public static class QuoteDto {

        /** The quote text. */
        @Nullable
        @SerializedName("text")
        private String text;

        /** The source reference (book title, author, etc.). */
        @Nullable
        @SerializedName("reference")
        private String reference;

        @Nullable public String getText() { return text; }
        @Nullable public String getReference() { return reference; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents a translation of one sense into another language.
     *
     * <p>Maps to the {@code Translation} schema in the OpenAPI spec.
     * Only present when the API request includes {@code ?translations=true}.</p>
     */
    public static class TranslationDto {

        /** The target language for this translation. */
        @Nullable
        @SerializedName("language")
        private LanguageDto language;

        /** The translated word or phrase. */
        @Nullable
        @SerializedName("word")
        private String word;

        @Nullable public LanguageDto getLanguage() { return language; }
        @Nullable public String getWord() { return word; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents attribution and licensing metadata for the API response.
     *
     * <p>Maps to the {@code Source} schema in the OpenAPI spec.</p>
     */
    public static class SourceDto {

        /** URL of the original Wiktionary page. */
        @Nullable
        @SerializedName("url")
        private String url;

        /** License information for the returned data. */
        @Nullable
        @SerializedName("license")
        private LicenseDto license;

        @Nullable public String getUrl() { return url; }
        @Nullable public LicenseDto getLicense() { return license; }
    }

    // -------------------------------------------------------------------------

    /**
     * Represents license metadata attached to the API response.
     *
     * <p>Maps to the {@code License} schema in the OpenAPI spec.</p>
     */
    public static class LicenseDto {

        /** License name (e.g. "CC BY-SA 4.0"). */
        @Nullable
        @SerializedName("name")
        private String name;

        /** URL to the full license text. */
        @Nullable
        @SerializedName("url")
        private String url;

        @Nullable public String getName() { return name; }
        @Nullable public String getUrl() { return url; }
    }
}
