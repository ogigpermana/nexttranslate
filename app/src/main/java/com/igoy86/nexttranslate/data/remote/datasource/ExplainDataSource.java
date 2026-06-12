package com.igoy86.nexttranslate.data.remote.datasource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igoy86.nexttranslate.data.remote.api.TranslateApiService;
import com.igoy86.nexttranslate.data.remote.dto.ExplainRequest;
import com.igoy86.nexttranslate.data.remote.dto.ExplainResponse;

import retrofit2.Call;

/**
 * Remote data source responsible for executing word-explanation requests
 * against the NextTranslate Vercel backend via Retrofit.
 *
 * <p>This class belongs to the data layer and acts as a thin wrapper around
 * {@link TranslateApiService}. It constructs the Authorization header and the
 * {@link ExplainRequest} body, then delegates the actual HTTP call to Retrofit.</p>
 *
 * <p>Callers receive a raw Retrofit {@link Call} and are responsible for
 * enqueueing or executing it on an appropriate thread. The
 * {@link com.igoy86.nexttranslate.data.repository.ExplainRepositoryImpl}
 * handles threading via {@link com.igoy86.nexttranslate.util.AppExecutors}.</p>
 *
 * <p>Instantiated by {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class ExplainDataSource {

    /** Tag used for logging events originating from this data source. */
    private static final String TAG = "ExplainDataSource";

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
     * Constructs a new {@link ExplainDataSource}.
     *
     * @param apiService the Retrofit service instance; must not be null
     * @param appToken   the raw secret token used for Bearer authentication; must not be null
     */
    public ExplainDataSource(
            @NonNull TranslateApiService apiService,
            @NonNull String appToken
    ) {
        this.apiService = apiService;
        this.appToken = appToken;
    }

    /**
     * Creates a Retrofit {@link Call} for explaining the given word.
     *
     * <p>The Authorization header is automatically prefixed with {@code "Bearer "}.</p>
     *
     * @param word       the word to explain; must not be null or empty
     * @param language   the full English name of the word's language (e.g. "English")
     * @param definition an optional short definition for context; may be null
     * @return a Retrofit {@link Call} that can be enqueued or executed
     */
    @NonNull
    public Call<ExplainResponse> explain(
            @NonNull String word,
            @NonNull String language,
            @Nullable String definition
    ) {
        final String authHeader = BEARER_PREFIX + appToken;
        final ExplainRequest request = new ExplainRequest(word, language, definition);
        return apiService.explain(authHeader, request);
    }
}
