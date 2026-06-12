package com.igoy86.nexttranslate.domain.usecase.history;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.HistoryRepository;

/**
 * Use case responsible for updating an existing translation history entry
 * in-place when the user continues editing text within the same session.
 *
 * <p>This use case is the second half of the upsert pattern used to
 * replicate Google Translate's history behaviour: instead of inserting
 * a new row every time the debounce fires, the ViewModel calls this use
 * case to overwrite the previously inserted entry with the latest text
 * and result — keeping one entry per translate session.</p>
 *
 * <p>Only {@code sourceText}, {@code translatedText},
 * {@code sourceLanguageCode}, and {@code timestamp} are updated.
 * {@code targetLanguageCode} is intentionally immutable per entry —
 * a language change always resets the session and triggers a fresh INSERT
 * via {@link AddHistoryUseCase}.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link HistoryRepository} interface, not its implementation.</p>
 *
 * <p>Usage example (in TranslateViewModel):</p>
 * <pre>
 *     // Second translate in the same session — update, not insert
 *     updateHistoryUseCase.execute(
 *         lastHistoryId,
 *         result.getSourceText(),
 *         result.getTranslatedText(),
 *         result.getSourceLanguageCode(),
 *         result.getTimestamp()
 *     );
 * </pre>
 */
public class UpdateHistoryUseCase {

    /** Repository used to persist the history update. */
    @NonNull
    private final HistoryRepository historyRepository;

    /**
     * Constructs a new {@link UpdateHistoryUseCase} with the given repository.
     *
     * @param historyRepository the repository used to update history entries;
     *                          must not be null
     */
    public UpdateHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by updating the history entry identified by
     * {@code id} with the latest translation content.
     *
     * <p>The actual database write is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.
     * This method returns immediately without blocking the caller.</p>
     *
     * @param id                 the database ID of the entry to update; must be &gt; 0
     * @param sourceText         the updated original source text; must not be null
     * @param translatedText     the updated translated result; must not be null
     * @param sourceLanguageCode the updated source language BCP-47 code; must not be null
     * @param timestamp          the updated Unix timestamp in milliseconds
     */
    public void execute(
            long id,
            @NonNull String sourceText,
            @NonNull String translatedText,
            @NonNull String sourceLanguageCode,
            long timestamp
    ) {
        historyRepository.updateHistory(id, sourceText, translatedText,
                sourceLanguageCode, timestamp);
    }
}