package com.igoy86.nexttranslate.data.remote.datasource;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.remote.api.TranslateApiService;
import com.igoy86.nexttranslate.data.remote.dto.TranslateRequest;
import com.igoy86.nexttranslate.data.remote.dto.TranslateResponse;

import retrofit2.Call;

/**
 * Remote data source responsible for executing translation requests against
 * the NextTranslate Vercel backend via Retrofit.
 *
 * <p>This class belongs to the data layer and acts as a thin wrapper around
 * {@link TranslateApiService}. It constructs the Authorization header and the
 * {@link TranslateRequest} body, then delegates the actual HTTP call to Retrofit.</p>
 *
 * <p>Callers receive a raw Retrofit {@link Call} and are responsible for
 * enqueueing or executing it on an appropriate thread. The
 * {@link com.igoy86.nexttranslate.data.repository.RemoteTranslateRepositoryImpl}
 * handles threading via {@link com.igoy86.nexttranslate.util.AppExecutors}.</p>
 *
 * <p>Instantiated by {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class RemoteTranslateDataSource {

    /** Tag used for logging events originating from this data source. */
    private static final String TAG = "RemoteTranslateDataSource";

    /** Bearer token prefix prepended to the raw app token. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** Retrofit service used to perform the API call. */
    @NonNull
    private final TranslateApiService apiService;

    /**
     * The raw secret token matching {@code APP_TOKEN} on the Vercel backend.
     * Stored here so callers do not need to manage it themselves.
     */
    @NonNull
    private final String appToken;

    /**
     * Constructs a new {@link RemoteTranslateDataSource}.
     *
     * @param apiService the Retrofit service instance; must not be null
     * @param appToken   the raw secret token used for Bearer authentication; must not be null
     */
    public RemoteTranslateDataSource(
            @NonNull TranslateApiService apiService,
            @NonNull String appToken
    ) {
        this.apiService = apiService;
        this.appToken = appToken;
    }

    /**
     * Creates a Retrofit {@link Call} for translating the given text.
     *
     * <p>The Authorization header is automatically prefixed with {@code "Bearer "}.</p>
     *
     * @param text       the source text to translate; must not be null or empty
     * @param sourceLang the full name of the source language (e.g. "Indonesian")
     * @param targetLang the full name of the target language (e.g. "English")
     * @return a Retrofit {@link Call} that can be enqueued or executed
     */
    @NonNull
    public Call<TranslateResponse> translate(
            @NonNull String text,
            @NonNull String sourceLang,
            @NonNull String targetLang
    ) {
        final String authHeader = BEARER_PREFIX + appToken;
        final TranslateRequest request = new TranslateRequest(text, sourceLang, targetLang);
        return apiService.translate(authHeader, request);
    }
}
