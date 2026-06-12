package com.igoy86.nexttranslate.data.remote.datasource;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.remote.api.DictionaryApiService;
import com.igoy86.nexttranslate.data.remote.dto.DictionaryResponseDto;

import retrofit2.Call;

/**
 * Remote data source responsible for executing dictionary lookup requests
 * against the Free Dictionary API via Retrofit.
 *
 * <p>This class belongs to the data layer and acts as a thin wrapper around
 * {@link DictionaryApiService}. It encapsulates the default language code
 * and delegates the HTTP call construction to Retrofit.</p>
 *
 * <p>Callers receive a raw Retrofit {@link Call} and are responsible for
 * enqueueing or executing it on an appropriate thread. The
 * {@link com.igoy86.nexttranslate.data.repository.DictionaryRepositoryImpl}
 * handles threading via {@link com.igoy86.nexttranslate.util.AppExecutors}.</p>
 *
 * <p>Instantiated by {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class DictionaryRemoteDataSource {

    /** Tag used for logging events originating from this data source. */
    private static final String TAG = "DictionaryRemoteDataSource";

    /**
     * Default language code used when no language is explicitly specified.
     * The Free Dictionary API uses ISO 639-1 codes (e.g. "en" for English).
     */
    private static final String DEFAULT_LANGUAGE = "all";

    /** Retrofit service used to perform dictionary API calls. */
    @NonNull
    private final DictionaryApiService apiService;

    /**
     * Constructs a new {@link DictionaryRemoteDataSource}.
     *
     * @param apiService the Retrofit service instance; must not be null
     */
    public DictionaryRemoteDataSource(@NonNull DictionaryApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Creates a Retrofit {@link Call} for looking up the given word in English.
     *
     * <p>Uses the default language code {@value #DEFAULT_LANGUAGE}.</p>
     *
     * @param word the word to look up; must not be null or empty
     * @return a Retrofit {@link Call} that can be enqueued or executed
     */
    @NonNull
    public Call<DictionaryResponseDto> lookupWord(@NonNull String word) {
        return apiService.getEntries(DEFAULT_LANGUAGE, word.trim().toLowerCase());
    }

    /**
     * Creates a Retrofit {@link Call} for looking up the given word in the
     * specified language.
     *
     * @param language ISO 639-1/639-3 language code (e.g. "en", "id", "fr")
     * @param word     the word to look up; must not be null or empty
     * @return a Retrofit {@link Call} that can be enqueued or executed
     */
    @NonNull
    public Call<DictionaryResponseDto> lookupWordInLanguage(
            @NonNull String language,
            @NonNull String word
    ) {
        return apiService.getEntries(language, word.trim().toLowerCase());
    }

    /**
     * Creates a Retrofit {@link Call} for looking up the given word with
     * per-sense translations included in the response.
     *
     * @param language     ISO 639-1/639-3 language code; must not be null
     * @param word         the word to look up; must not be null or empty
     * @return a Retrofit {@link Call} that can be enqueued or executed
     */
    @NonNull
    public Call<DictionaryResponseDto> lookupWordWithTranslations(
            @NonNull String language,
            @NonNull String word
    ) {
        return apiService.getEntriesWithTranslations(
                language,
                word.trim().toLowerCase(),
                true
        );
    }
}
