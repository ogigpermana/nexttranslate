package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.igoy86.nexttranslate.data.remote.datasource.ExplainDataSource;
import com.igoy86.nexttranslate.data.remote.dto.ExplainResponse;
import com.igoy86.nexttranslate.domain.repository.ExplainRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Concrete implementation of {@link ExplainRepository}.
 *
 * <p>Executes word-explanation requests against the NextTranslate Vercel backend
 * (Groq + LLaMA) via {@link ExplainDataSource} and Retrofit.</p>
 *
 * <p>Network calls are made asynchronously using Retrofit's
 * {@link Call#enqueue(Callback)} mechanism. Results are posted back to the
 * main thread via {@link MutableLiveData#postValue(Object)}, making them
 * safe to observe directly from the presentation layer.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class ExplainRepositoryImpl implements ExplainRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "ExplainRepositoryImpl";

    /** Data source used to execute explain API calls via Retrofit. */
    @NonNull
    private final ExplainDataSource explainDataSource;

    /** Executor pools used for background and main-thread operations. */
    @NonNull
    private final AppExecutors appExecutors;

    /**
     * Constructs a new {@link ExplainRepositoryImpl}.
     *
     * @param explainDataSource the Retrofit-backed data source; must not be null
     * @param appExecutors      the executor pools for threading; must not be null
     */
    public ExplainRepositoryImpl(
            @NonNull ExplainDataSource explainDataSource,
            @NonNull AppExecutors appExecutors
    ) {
        this.explainDataSource = explainDataSource;
        this.appExecutors = appExecutors;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Emits {@link Resource#loading(Object)} immediately on the calling thread,
     * then enqueues a Retrofit call. On success, extracts the explanation string
     * and emits {@link Resource#success(Object)}. On any failure, emits
     * {@link Resource#error(String, Object)} with a human-readable message.</p>
     */
    @NonNull
    @Override
    public LiveData<Resource<String>> explain(
            @NonNull String word,
            @NonNull String language,
            @Nullable String definition
    ) {
        final MutableLiveData<Resource<String>> resultLiveData = new MutableLiveData<>();

        // Emit loading state immediately before the network call begins
        resultLiveData.setValue(Resource.loading(null));

        FileLogger.d(TAG, "Starting explain request: word=" + word + " lang=" + language);

        explainDataSource.explain(word, language, definition)
                .enqueue(new Callback<ExplainResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<ExplainResponse> call,
                            @NonNull Response<ExplainResponse> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            final ExplainResponse body = response.body();

                            if (body.isSuccess() && body.getData() != null) {
                                final String en = body.getData().getExplanation();
                                final String id = body.getData().getExplanationId();
                                final String combined = en + "\n\n---\n\n" + (id != null ? id : "");
                                FileLogger.d(TAG, "Explain success: word=" + word);
                                resultLiveData.postValue(Resource.success(combined));
                            } else {
                                final String errorMsg = body.getError() != null
                                        ? body.getError()
                                        : "Unknown backend error.";
                                FileLogger.e(TAG, "Backend error: " + errorMsg);
                                resultLiveData.postValue(Resource.error(errorMsg, null));
                            }
                        } else {
                            final String httpError = "HTTP " + response.code()
                                    + ": " + response.message();
                            FileLogger.e(TAG, "HTTP error: " + httpError);
                            resultLiveData.postValue(Resource.error(httpError, null));
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ExplainResponse> call,
                            @NonNull Throwable t
                    ) {
                        FileLogger.e(TAG, "Network failure during explain.", t);
                        resultLiveData.postValue(
                                Resource.error("Network error: " + t.getMessage(), null)
                        );
                    }
                });

        return resultLiveData;
    }
}
