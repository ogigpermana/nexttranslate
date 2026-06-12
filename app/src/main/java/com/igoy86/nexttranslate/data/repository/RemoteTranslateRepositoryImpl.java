package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.igoy86.nexttranslate.data.remote.datasource.RemoteTranslateDataSource;
import com.igoy86.nexttranslate.data.remote.dto.TranslateResponse;
import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.domain.repository.RemoteTranslateRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Concrete implementation of {@link RemoteTranslateRepository}.
 *
 * <p>Executes translation requests against the NextTranslate Vercel backend
 * (Groq + LLaMA) via {@link RemoteTranslateDataSource} and Retrofit.</p>
 *
 * <p>Network calls are made asynchronously using Retrofit's
 * {@link Call#enqueue(Callback)} mechanism. Results are posted back to the
 * main thread via {@link MutableLiveData#postValue(Object)}, making them
 * safe to observe directly from the presentation layer.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class RemoteTranslateRepositoryImpl implements RemoteTranslateRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "RemoteTranslateRepositoryImpl";

    /** Data source used to execute translation API calls via Retrofit. */
    @NonNull
    private final RemoteTranslateDataSource remoteDataSource;

    /** Executor pools used for background and main-thread operations. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link RemoteTranslateRepositoryImpl}.
     *
     * @param remoteDataSource the Retrofit-backed data source; must not be null
     * @param appExecutors     the executor pools for threading; must not be null
     */
    public RemoteTranslateRepositoryImpl(
            @NonNull RemoteTranslateDataSource remoteDataSource,
            @NonNull AppExecutors appExecutors
    ) {
        this.remoteDataSource = remoteDataSource;
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // RemoteTranslateRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Emits {@link Resource#loading(Object)} immediately on the calling thread,
     * then enqueues a Retrofit call. On success, maps the {@link TranslateResponse}
     * to a {@link TranslationResult} domain model and emits
     * {@link Resource#success(Object)}. On any failure, emits
     * {@link Resource#error(String, Object)} with a human-readable message.</p>
     */
    @NonNull
    @Override
    public LiveData<Resource<TranslationResult>> translate(
            @NonNull String sourceText,
            @NonNull String sourceLang,
            @NonNull String targetLang
    ) {
        final MutableLiveData<Resource<TranslationResult>> resultLiveData =
                new MutableLiveData<>();

        // Emit loading state immediately before the network call begins
        resultLiveData.setValue(Resource.loading(null));

        FileLogger.d(TAG, "Starting remote translation: " + sourceLang + " → " + targetLang);

        remoteDataSource.translate(sourceText, sourceLang, targetLang)
                .enqueue(new Callback<TranslateResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<TranslateResponse> call,
                            @NonNull Response<TranslateResponse> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            final TranslateResponse body = response.body();

                            if (body.isSuccess() && body.getData() != null) {
                                // Map response DTO → domain model
                                final TranslationResult result = new TranslationResult(
                                        body.getData().getOriginalText(),
                                        body.getData().getTranslatedText(),
                                        sourceLang,
                                        targetLang,
                                        System.currentTimeMillis()
                                );
                                FileLogger.d(TAG, "Remote translation success: "
                                        + body.getData().getTranslatedText());
                                resultLiveData.postValue(Resource.success(result));
                            } else {
                                // Backend returned success=false with an error message
                                final String errorMsg = body.getError() != null
                                        ? body.getError()
                                        : "Unknown backend error.";
                                FileLogger.e(TAG, "Backend error: " + errorMsg);
                                resultLiveData.postValue(Resource.error(errorMsg, null));
                            }
                        } else {
                            // HTTP error (4xx / 5xx)
                            final String httpError = "HTTP " + response.code()
                                    + ": " + response.message();
                            FileLogger.e(TAG, "HTTP error: " + httpError);
                            resultLiveData.postValue(Resource.error(httpError, null));
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<TranslateResponse> call,
                            @NonNull Throwable t
                    ) {
                        // Network failure (no internet, timeout, etc.)
                        FileLogger.e(TAG, "Network failure during translation.", t);
                        resultLiveData.postValue(
                                Resource.error("Network error: " + t.getMessage(), null)
                        );
                    }
                });

        return resultLiveData;
    }
}
