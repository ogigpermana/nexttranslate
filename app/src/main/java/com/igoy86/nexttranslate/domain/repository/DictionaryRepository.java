package com.igoy86.nexttranslate.domain.repository;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.DictionaryEntry;

import java.util.List;

/**
 * Repository interface defining the contract for dictionary word lookup operations.
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Retrofit or any Android framework classes directly. The actual implementation
 * resides in the data layer
 * ({@link com.igoy86.nexttranslate.data.repository.DictionaryRepositoryImpl}).</p>
 *
 * <p>All operations are asynchronous and deliver results via the
 * {@link DictionaryCallback} interface. Implementations dispatch network
 * requests on a background thread via
 * {@link com.igoy86.nexttranslate.util.AppExecutors} and invoke the callback
 * on the same background thread — callers that need to update the UI must
 * post back to the main thread.</p>
 */
public interface DictionaryRepository {

    /**
     * Looks up the given word in English using the Free Dictionary API.
     *
     * <p>On success, {@link DictionaryCallback#onSuccess} is called with a
     * non-empty list of {@link DictionaryEntry} objects — one per
     * language/part-of-speech combination found for the word.</p>
     *
     * <p>On failure (word not found, network error, etc.),
     * {@link DictionaryCallback#onError} is called with a human-readable
     * error message.</p>
     *
     * @param word     the word to look up; must not be null or empty
     * @param callback delivers the result or error; must not be null
     */
    void lookupWord(@NonNull String word, @NonNull DictionaryCallback callback);

    /**
     * Looks up the given word in the specified language using the
     * Free Dictionary API.
     *
     * @param language ISO 639-1/639-3 language code (e.g. "en", "id");
     *                 must not be null
     * @param word     the word to look up; must not be null or empty
     * @param callback delivers the result or error; must not be null
     */
    void lookupWordInLanguage(
            @NonNull String language,
            @NonNull String word,
            @NonNull DictionaryCallback callback
    );

    // =========================================================================
    // Callback interface
    // =========================================================================

    /**
     * Callback interface for receiving dictionary lookup results asynchronously.
     *
     * <p>Implementations are invoked on a background thread. Callers that need
     * to update ViewModel state or UI must post back to the main thread via
     * {@link com.igoy86.nexttranslate.util.AppExecutors#mainThread()} or
     * a {@code Handler}.</p>
     */
    interface DictionaryCallback {

        /**
         * Called when the dictionary lookup completes successfully.
         *
         * @param entries a non-empty list of {@link DictionaryEntry} objects
         *                for the requested word; never null
         */
        void onSuccess(@NonNull List<DictionaryEntry> entries);

        /**
         * Called when the dictionary lookup fails.
         *
         * <p>Common failure reasons:</p>
         * <ul>
         *     <li>Word not found in the dictionary (HTTP 404)</li>
         *     <li>Network unavailable or timeout</li>
         *     <li>Unexpected API error (HTTP 5xx)</li>
         * </ul>
         *
         * @param errorMessage a human-readable description of the failure;
         *                     never null
         */
        void onError(@NonNull String errorMessage);
    }
}
