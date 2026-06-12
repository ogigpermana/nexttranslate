package com.igoy86.nexttranslate.domain.usecase.dictionary;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.DictionaryRepository;

/**
 * Use case for performing a dictionary word lookup via the Free Dictionary API.
 *
 * <p>This use case belongs to the domain layer. It encapsulates the business
 * rule of looking up a word and delegates the actual network operation to
 * {@link DictionaryRepository}.</p>
 *
 * <p>Instantiated and provided by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 *
 * <p>Usage example in a ViewModel:</p>
 * <pre>
 *     lookupWordUseCase.execute("hello", new DictionaryRepository.DictionaryCallback() {
 *         {@literal @}Override
 *         public void onSuccess(List{@literal <}DictionaryEntry{@literal >} entries) {
 *             appExecutors.mainThread().execute(() -> resultLiveData.setValue(entries));
 *         }
 *
 *         {@literal @}Override
 *         public void onError(String errorMessage) {
 *             appExecutors.mainThread().execute(() -> errorLiveData.setValue(errorMessage));
 *         }
 *     });
 * </pre>
 */
public class LookupWordUseCase {

    /** Repository used to perform the word lookup operation. */
    @NonNull
    private final DictionaryRepository dictionaryRepository;

    /**
     * Constructs a new {@link LookupWordUseCase}.
     *
     * @param dictionaryRepository the repository for dictionary operations;
     *                             must not be null
     */
    public LookupWordUseCase(@NonNull DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    /**
     * Executes the word lookup in English (default language).
     *
     * <p>Results are delivered asynchronously via {@code callback} on a
     * background thread. Callers must post back to the main thread if
     * updating the UI.</p>
     *
     * @param word     the word to look up; must not be null or empty
     * @param callback delivers the result or error; must not be null
     */
    public void execute(
            @NonNull String word,
            @NonNull DictionaryRepository.DictionaryCallback callback
    ) {
        dictionaryRepository.lookupWord(word, callback);
    }

    /**
     * Executes the word lookup in a specific language.
     *
     * <p>Results are delivered asynchronously via {@code callback} on a
     * background thread. Callers must post back to the main thread if
     * updating the UI.</p>
     *
     * @param language ISO 639-1/639-3 language code (e.g. "en", "id");
     *                 must not be null
     * @param word     the word to look up; must not be null or empty
     * @param callback delivers the result or error; must not be null
     */
    public void execute(
            @NonNull String language,
            @NonNull String word,
            @NonNull DictionaryRepository.DictionaryCallback callback
    ) {
        dictionaryRepository.lookupWordInLanguage(language, word, callback);
    }
}
