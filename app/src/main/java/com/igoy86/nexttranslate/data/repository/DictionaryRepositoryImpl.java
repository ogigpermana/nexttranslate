package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.mapper.DictionaryMapper;
import com.igoy86.nexttranslate.data.remote.datasource.DictionaryRemoteDataSource;
import com.igoy86.nexttranslate.data.remote.dto.DictionaryResponseDto;
import com.igoy86.nexttranslate.domain.model.DictionaryEntry;
import com.igoy86.nexttranslate.domain.repository.DictionaryRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Concrete implementation of {@link DictionaryRepository}.
 *
 * <p>Handles dictionary word lookup requests by delegating HTTP calls to
 * {@link DictionaryRemoteDataSource} and mapping the raw DTO response to
 * domain models via {@link DictionaryMapper}.</p>
 *
 * <p>All network requests are dispatched to a background network thread via
 * {@link AppExecutors#networkIO()} to avoid blocking the main thread.
 * The callback is invoked on the same background thread — callers responsible
 * for updating ViewModel state must post back to the main thread via
 * {@link AppExecutors#mainThread()}.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class DictionaryRepositoryImpl implements DictionaryRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "DictionaryRepositoryImpl";

    /** Error message returned when the word is not found (HTTP 404). */
    private static final String ERROR_WORD_NOT_FOUND = "Word not found in dictionary.";

    /** Error message returned when a network failure occurs. */
    private static final String ERROR_NETWORK = "Network error. Please check your connection.";

    /** Error message returned when an unexpected API error occurs. */
    private static final String ERROR_UNEXPECTED = "Unexpected error occurred.";

    /** Remote data source used to execute dictionary API calls. */
    @NonNull
    private final DictionaryRemoteDataSource remoteDataSource;

    /** Executor pools used to dispatch background and main-thread tasks. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link DictionaryRepositoryImpl} with the required dependencies.
     *
     * @param remoteDataSource the data source for dictionary API calls; must not be null
     * @param appExecutors     the executor pools for background threading; must not be null
     */
    public DictionaryRepositoryImpl(
            @NonNull DictionaryRemoteDataSource remoteDataSource,
            @NonNull AppExecutors appExecutors
    ) {
        this.remoteDataSource = remoteDataSource;
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // DictionaryRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the network call on a background thread via
     * {@link AppExecutors#networkIO()}, then maps the response DTO to domain
     * models and delivers the result via {@code callback}.</p>
     */
    @Override
    public void lookupWord(
            @NonNull String word,
            @NonNull DictionaryCallback callback
    ) {
        appExecutors.networkIO().execute(() -> {
            try {
                final Response<DictionaryResponseDto> response =
                        remoteDataSource.lookupWord(word).execute();
                handleResponse(word, response, callback);
            } catch (IOException ex) {
                FileLogger.e(TAG, "Network error looking up word: " + word, ex);
                callback.onError(ERROR_NETWORK);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Unexpected error looking up word: " + word, ex);
                callback.onError(ERROR_UNEXPECTED);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the network call on a background thread via
     * {@link AppExecutors#networkIO()}, then maps the response DTO to domain
     * models and delivers the result via {@code callback}.</p>
     */
    @Override
    public void lookupWordInLanguage(
            @NonNull String language,
            @NonNull String word,
            @NonNull DictionaryCallback callback
    ) {
        appExecutors.networkIO().execute(() -> {
            try {
                final Response<DictionaryResponseDto> response =
                        remoteDataSource.lookupWordInLanguage(language, word).execute();
                handleResponse(word, response, callback);
            } catch (IOException ex) {
                FileLogger.e(TAG, "Network error looking up word: " + word
                        + " lang: " + language, ex);
                callback.onError(ERROR_NETWORK);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Unexpected error looking up word: " + word, ex);
                callback.onError(ERROR_UNEXPECTED);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Handles a raw Retrofit {@link Response} by checking the HTTP status code
     * and either mapping the body to domain models or delivering an error.
     *
     * @param word     the word that was looked up (used for logging)
     * @param response the Retrofit response; must not be null
     * @param callback delivers success or error to the caller
     */
    private void handleResponse(
            @NonNull String word,
            @NonNull Response<DictionaryResponseDto> response,
            @NonNull DictionaryCallback callback
    ) {
        if (response.isSuccessful() && response.body() != null) {
            final List<DictionaryEntry> entries =
                    DictionaryMapper.toDomainList(response.body());

            if (entries.isEmpty()) {
                FileLogger.d(TAG, "Word found but no entries returned: " + word);
                callback.onError(ERROR_WORD_NOT_FOUND);
            } else {
                FileLogger.d(TAG, "Word lookup success: " + word
                        + " entries=" + entries.size());
                callback.onSuccess(entries);
            }
        } else if (response.code() == 404) {
            FileLogger.d(TAG, "Word not found (404): " + word);
            callback.onError(ERROR_WORD_NOT_FOUND);
        } else {
            FileLogger.e(TAG, "API error " + response.code() + " for word: " + word, null);
            callback.onError(ERROR_UNEXPECTED + " (HTTP " + response.code() + ")");
        }
    }
}
