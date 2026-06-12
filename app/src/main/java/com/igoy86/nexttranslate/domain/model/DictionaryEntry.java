package com.igoy86.nexttranslate.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Domain model representing the result of a dictionary word lookup.
 *
 * <p>This is a pure Java model class with no Android or framework dependencies,
 * keeping the domain layer clean and testable.</p>
 *
 * <p>A {@link DictionaryEntry} aggregates all the information returned for a
 * single word by the Free Dictionary API: the word itself, its language,
 * part of speech, phonetic pronunciation, inflected forms, meanings (senses),
 * and entry-level synonyms and antonyms.</p>
 *
 * <p>A single word lookup may produce multiple {@link DictionaryEntry} objects
 * — one per language/part-of-speech combination. For example, the word "book"
 * may have separate entries for "noun" and "verb" senses.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     DictionaryEntry entry = new DictionaryEntry(
 *         "hello",
 *         "en", "English",
 *         "interjection",
 *         "/həˈloʊ/",
 *         senseList,
 *         synonymList,
 *         antonymList
 *     );
 * </pre>
 */
public class DictionaryEntry {

    /** The word that was looked up. */
    @NonNull
    private final String word;

    /** ISO 639-1/639-3 language code (e.g. "en", "id"). */
    @NonNull
    private final String languageCode;

    /** Full English name of the language (e.g. "English"). */
    @NonNull
    private final String languageName;

    /**
     * Grammatical category of this entry (e.g. "noun", "verb", "adjective").
     * May be empty if the API did not provide this information.
     */
    @NonNull
    private final String partOfSpeech;

    /**
     * Primary IPA pronunciation string (e.g. "/həˈloʊ/").
     * May be null if no pronunciation data is available.
     */
    @Nullable
    private final String pronunciation;

    /**
     * All meanings (senses) for this entry, each with a definition,
     * example sentences, synonyms, and antonyms.
     */
    @NonNull
    private final List<Sense> senses;

    /**
     * Entry-level synonyms that apply across all senses.
     * May be empty.
     */
    @NonNull
    private final List<String> synonyms;

    /**
     * Entry-level antonyms that apply across all senses.
     * May be empty.
     */
    @NonNull
    private final List<String> antonyms;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link DictionaryEntry} with all required fields.
     *
     * @param word          the word that was looked up
     * @param languageCode  ISO 639-1/639-3 language code
     * @param languageName  full English language name
     * @param partOfSpeech  grammatical category
     * @param pronunciation IPA pronunciation string, or null if unavailable
     * @param senses        list of meanings; must not be null
     * @param synonyms      entry-level synonyms; must not be null
     * @param antonyms      entry-level antonyms; must not be null
     */
    public DictionaryEntry(
            @NonNull String word,
            @NonNull String languageCode,
            @NonNull String languageName,
            @NonNull String partOfSpeech,
            @Nullable String pronunciation,
            @NonNull List<Sense> senses,
            @NonNull List<String> synonyms,
            @NonNull List<String> antonyms
    ) {
        this.word = word;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.partOfSpeech = partOfSpeech;
        this.pronunciation = pronunciation;
        this.senses = senses;
        this.synonyms = synonyms;
        this.antonyms = antonyms;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** @return the word that was looked up; never null */
    @NonNull public String getWord() { return word; }

    /** @return ISO 639-1/639-3 language code; never null */
    @NonNull public String getLanguageCode() { return languageCode; }

    /** @return full English language name; never null */
    @NonNull public String getLanguageName() { return languageName; }

    /** @return grammatical category; never null, may be empty */
    @NonNull public String getPartOfSpeech() { return partOfSpeech; }

    /** @return primary IPA pronunciation, or null if unavailable */
    @Nullable public String getPronunciation() { return pronunciation; }

    /** @return list of meanings; never null */
    @NonNull public List<Sense> getSenses() { return senses; }

    /** @return entry-level synonyms; never null */
    @NonNull public List<String> getSynonyms() { return synonyms; }

    /** @return entry-level antonyms; never null */
    @NonNull public List<String> getAntonyms() { return antonyms; }

    // =========================================================================
    // Nested domain model: Sense
    // =========================================================================

    /**
     * Represents one specific meaning of the word.
     *
     * <p>Each sense contains a definition, optional usage examples,
     * optional synonyms/antonyms scoped to that specific meaning,
     * and optional nested sub-senses for more granular distinctions.</p>
     */
    public static class Sense {

        /** The definition text for this meaning. */
        @NonNull
        private final String definition;

        /** Usage tags (e.g. "formal", "archaic", "technical"). May be empty. */
        @NonNull
        private final List<String> tags;

        /** Example sentences demonstrating this meaning. May be empty. */
        @NonNull
        private final List<String> examples;

        /** Synonyms specific to this sense. May be empty. */
        @NonNull
        private final List<String> synonyms;

        /** Antonyms specific to this sense. May be empty. */
        @NonNull
        private final List<String> antonyms;

        /** More specific sub-meanings within this sense. May be empty. */
        @NonNull
        private final List<Sense> subsenses;

        /**
         * Constructs a new {@link Sense}.
         *
         * @param definition the definition text; must not be null
         * @param tags       usage tags; must not be null
         * @param examples   example sentences; must not be null
         * @param synonyms   sense-level synonyms; must not be null
         * @param antonyms   sense-level antonyms; must not be null
         * @param subsenses  nested sub-senses; must not be null
         */
        public Sense(
                @NonNull String definition,
                @NonNull List<String> tags,
                @NonNull List<String> examples,
                @NonNull List<String> synonyms,
                @NonNull List<String> antonyms,
                @NonNull List<Sense> subsenses
        ) {
            this.definition = definition;
            this.tags = tags;
            this.examples = examples;
            this.synonyms = synonyms;
            this.antonyms = antonyms;
            this.subsenses = subsenses;
        }

        /** @return the definition text; never null */
        @NonNull public String getDefinition() { return definition; }

        /** @return usage tags; never null */
        @NonNull public List<String> getTags() { return tags; }

        /** @return example sentences; never null */
        @NonNull public List<String> getExamples() { return examples; }

        /** @return sense-level synonyms; never null */
        @NonNull public List<String> getSynonyms() { return synonyms; }

        /** @return sense-level antonyms; never null */
        @NonNull public List<String> getAntonyms() { return antonyms; }

        /** @return nested sub-senses; never null */
        @NonNull public List<Sense> getSubsenses() { return subsenses; }
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "DictionaryEntry{" +
                "word='" + word + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", partOfSpeech='" + partOfSpeech + '\'' +
                ", senses=" + senses.size() +
                '}';
    }
}
