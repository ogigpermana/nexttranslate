package com.igoy86.nexttranslate.data.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igoy86.nexttranslate.data.remote.dto.DictionaryResponseDto;
import com.igoy86.nexttranslate.domain.model.DictionaryEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper utility class responsible for converting between
 * {@link DictionaryResponseDto} (data layer) and {@link DictionaryEntry} (domain layer).
 *
 * <p>Mappers are a critical part of Clean Architecture — they prevent
 * data layer implementation details (Retrofit DTOs) from leaking into
 * the domain or presentation layers.</p>
 *
 * <p>This class is stateless and all methods are static. It should
 * never be instantiated.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     // DTO list → Domain list
 *     List{@literal <}DictionaryEntry{@literal >} entries =
 *         DictionaryMapper.toDomainList("hello", responseDto.getEntries());
 * </pre>
 */
public class DictionaryMapper {

    /**
     * Private constructor to prevent instantiation.
     * All methods in this class are static utility methods.
     */
    private DictionaryMapper() {
        throw new UnsupportedOperationException(
                "DictionaryMapper is a utility class and cannot be instantiated."
        );
    }

    // -------------------------------------------------------------------------
    // DTO → Domain
    // -------------------------------------------------------------------------

    /**
     * Converts a full {@link DictionaryResponseDto} into a list of
     * {@link DictionaryEntry} domain models.
     *
     * <p>Each {@link DictionaryResponseDto.EntryDto} in the response produces
     * one {@link DictionaryEntry}. The word string from the top-level
     * response is passed down to each entry.</p>
     *
     * <p>Returns an empty list if the response or its entries list is null.</p>
     *
     * @param response the full API response DTO; may be null
     * @return a non-null list of {@link DictionaryEntry} domain models
     */
    @NonNull
    public static List<DictionaryEntry> toDomainList(
            @Nullable DictionaryResponseDto response
    ) {
        if (response == null
                || response.getEntries() == null
                || response.getEntries().isEmpty()) {
            return new ArrayList<>();
        }
        return toDomainList(response.getWord(), response.getEntries());
    }

    /**
     * Converts a list of {@link DictionaryResponseDto.EntryDto} objects into
     * a list of {@link DictionaryEntry} domain models.
     *
     * <p>Returns an empty list if the input list is null or empty.</p>
     *
     * @param word    the word that was looked up; used in each domain model
     * @param entries the list of entry DTOs to convert; may be null
     * @return a non-null list of {@link DictionaryEntry} domain models
     */
    @NonNull
    public static List<DictionaryEntry> toDomainList(
            @NonNull String word,
            @Nullable List<DictionaryResponseDto.EntryDto> entries
    ) {
        final List<DictionaryEntry> result = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return result;
        }
        for (DictionaryResponseDto.EntryDto entryDto : entries) {
            result.add(toDomain(word, entryDto));
        }
        return result;
    }

    /**
     * Converts a single {@link DictionaryResponseDto.EntryDto} into a
     * {@link DictionaryEntry} domain model.
     *
     * <p>Null-safe: missing optional fields fall back to empty strings or
     * empty lists so the domain model is always fully initialized.</p>
     *
     * @param word     the word that was looked up
     * @param entryDto the entry DTO to convert; must not be null
     * @return the corresponding {@link DictionaryEntry} domain model
     */
    @NonNull
    public static DictionaryEntry toDomain(
            @NonNull String word,
            @NonNull DictionaryResponseDto.EntryDto entryDto
    ) {
        // Language
        final String langCode;
        final String langName;
        if (entryDto.getLanguage() != null) {
            langCode = safeString(entryDto.getLanguage().getCode());
            langName = safeString(entryDto.getLanguage().getName());
        } else {
            langCode = "";
            langName = "";
        }

        // Part of speech
        final String partOfSpeech = safeString(entryDto.getPartOfSpeech());

        // Pronunciation — pick the first IPA entry if available
        final String pronunciation = extractPrimaryPronunciation(
                entryDto.getPronunciations()
        );

        // Senses
        final List<DictionaryEntry.Sense> senses = mapSenses(entryDto.getSenses());

        // Synonyms / antonyms at entry level
        final List<String> synonyms = safeList(entryDto.getSynonyms());
        final List<String> antonyms = safeList(entryDto.getAntonyms());

        return new DictionaryEntry(
                word,
                langCode,
                langName,
                partOfSpeech,
                pronunciation,
                senses,
                synonyms,
                antonyms
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the primary pronunciation string from the pronunciations list.
     *
     * <p>Prefers IPA type. Falls back to the first available entry if no
     * IPA pronunciation is present. Returns null if the list is empty.</p>
     *
     * @param pronunciations the list of pronunciation DTOs; may be null
     * @return the IPA text string, or null if unavailable
     */
    @Nullable
    private static String extractPrimaryPronunciation(
            @Nullable List<DictionaryResponseDto.PronunciationDto> pronunciations
    ) {
        if (pronunciations == null || pronunciations.isEmpty()) {
            return null;
        }
        // Prefer IPA
        for (DictionaryResponseDto.PronunciationDto p : pronunciations) {
            if ("ipa".equalsIgnoreCase(p.getType()) && p.getText() != null) {
                return p.getText();
            }
        }
        // Fallback to first available
        return pronunciations.get(0).getText();
    }

    /**
     * Converts a list of {@link DictionaryResponseDto.SenseDto} objects into
     * a list of {@link DictionaryEntry.Sense} domain models.
     *
     * @param senseDtos the DTO list; may be null
     * @return a non-null list of domain {@link DictionaryEntry.Sense} objects
     */
    @NonNull
    private static List<DictionaryEntry.Sense> mapSenses(
            @Nullable List<DictionaryResponseDto.SenseDto> senseDtos
    ) {
        final List<DictionaryEntry.Sense> result = new ArrayList<>();
        if (senseDtos == null || senseDtos.isEmpty()) {
            return result;
        }
        for (DictionaryResponseDto.SenseDto senseDto : senseDtos) {
            result.add(mapSense(senseDto));
        }
        return result;
    }

    /**
     * Converts a single {@link DictionaryResponseDto.SenseDto} into a
     * {@link DictionaryEntry.Sense} domain model.
     *
     * <p>Recursively maps sub-senses.</p>
     *
     * @param senseDto the sense DTO to convert; must not be null
     * @return the corresponding {@link DictionaryEntry.Sense}
     */
    @NonNull
    private static DictionaryEntry.Sense mapSense(
            @NonNull DictionaryResponseDto.SenseDto senseDto
    ) {
        return new DictionaryEntry.Sense(
                safeString(senseDto.getDefinition()),
                safeList(senseDto.getTags()),
                safeList(senseDto.getExamples()),
                safeList(senseDto.getSynonyms()),
                safeList(senseDto.getAntonyms()),
                mapSenses(senseDto.getSubsenses())   // recursive
        );
    }

    /**
     * Returns the given string, or an empty string if it is null.
     *
     * @param value the string to check
     * @return the original string or {@code ""}
     */
    @NonNull
    private static String safeString(@Nullable String value) {
        return value != null ? value : "";
    }

    /**
     * Returns the given list, or an empty unmodifiable list if it is null.
     *
     * @param list the list to check; may be null
     * @param <T>  the list element type
     * @return the original list or {@link Collections#emptyList()}
     */
    @NonNull
    private static <T> List<T> safeList(@Nullable List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
