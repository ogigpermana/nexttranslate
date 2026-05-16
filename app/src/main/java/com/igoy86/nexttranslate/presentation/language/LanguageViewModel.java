package com.igoy86.nexttranslate.presentation.language;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.igoy86.nexttranslate.domain.model.DownloadProgress;
import com.igoy86.nexttranslate.domain.model.LanguageModel;
import com.igoy86.nexttranslate.domain.usecase.language.DeleteLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.DownloadLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.GetDownloadedLanguagesUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

import java.util.List;

/**
 * ViewModel for the Language Manager screen.
 *
 * <p>Manages the UI state for displaying all supported languages,
 * downloading ML Kit translation models, and deleting previously
 * downloaded models. Survives configuration changes and exposes
 * state via {@link LiveData}.</p>
 *
 * <p>Depends on:</p>
 * <ul>
 *     <li>{@link GetDownloadedLanguagesUseCase} — retrieves all languages with status</li>
 *     <li>{@link DownloadLanguageModelUseCase} — downloads a language model</li>
 *     <li>{@link DeleteLanguageModelUseCase} — deletes a downloaded model</li>
 * </ul>
 *
 * <p>Instantiated via {@link LanguageViewModelFactory}.</p>
 */
public class LanguageViewModel extends BaseViewModel {

    /** Tag used for logging events originating from this ViewModel. */
    private static final String TAG = "LanguageViewModel";

    // -------------------------------------------------------------------------
    // Use Cases
    // -------------------------------------------------------------------------

    /** Use case for retrieving all supported languages with download status. */
    @NonNull
    private final GetDownloadedLanguagesUseCase getDownloadedLanguagesUseCase;

    /** Use case for downloading an ML Kit translation model. */
    @NonNull
    private final DownloadLanguageModelUseCase downloadLanguageModelUseCase;

    /** Use case for deleting a downloaded ML Kit translation model. */
    @NonNull
    private final DeleteLanguageModelUseCase deleteLanguageModelUseCase;

    // -------------------------------------------------------------------------
    // UI State LiveData
    // -------------------------------------------------------------------------

    /**
     * The full list of supported languages with their current download status.
     * Emits loading, success, or error states via {@link Resource}.
     */
    private final MutableLiveData<Resource<List<LanguageModel>>> languageListLiveData =
            new MutableLiveData<>();

    /**
     * The result of the most recent model download operation.
     * Emits the BCP-47 language code on success, or an error message on failure.
     */
    private final MutableLiveData<Resource<String>> downloadResultLiveData =
            new MutableLiveData<>();

    /**
     * The result of the most recent model deletion operation.
     * Emits the BCP-47 language code on success, or an error message on failure.
     */
    private final MutableLiveData<Resource<String>> deleteResultLiveData =
            new MutableLiveData<>();

    /**
     * The BCP-47 language code of the model currently being downloaded or deleted.
     * Used to show a per-item progress indicator in the RecyclerView.
     * {@code null} when no operation is in progress.
     */
    private final MutableLiveData<String> activeLanguageCodeLiveData =
            new MutableLiveData<>(null);

    /**
     * The current download progress for the active download operation.
     * Emits {@link DownloadProgress} while a model is downloading.
     * {@code null} when no download is in progress.
     */
    private final MutableLiveData<DownloadProgress> downloadProgressLiveData =
            new MutableLiveData<>(null);

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link LanguageViewModel} with the required use cases.
     *
     * <p>Immediately loads the language list on initialization so the
     * Language Manager screen is populated as soon as it opens.</p>
     *
     * @param getDownloadedLanguagesUseCase use case for retrieving languages; must not be null
     * @param downloadLanguageModelUseCase  use case for downloading models; must not be null
     * @param deleteLanguageModelUseCase    use case for deleting models; must not be null
     */
    public LanguageViewModel(
            @NonNull GetDownloadedLanguagesUseCase getDownloadedLanguagesUseCase,
            @NonNull DownloadLanguageModelUseCase downloadLanguageModelUseCase,
            @NonNull DeleteLanguageModelUseCase deleteLanguageModelUseCase
    ) {
        this.getDownloadedLanguagesUseCase = getDownloadedLanguagesUseCase;
        this.downloadLanguageModelUseCase = downloadLanguageModelUseCase;
        this.deleteLanguageModelUseCase = deleteLanguageModelUseCase;

        // Load language list immediately on ViewModel creation
        loadLanguages();
        FileLogger.d(TAG, "LanguageViewModel initialized.");
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Loads or refreshes the full list of supported languages with their
     * current download status from ML Kit.
     *
     * <p>Emits loading, success, or error states via
     * {@link #getLanguageListLiveData()}. Call this after a download or
     * delete operation completes to refresh the list.</p>
     */
    public void loadLanguages() {
        FileLogger.d(TAG, "Loading language list.");

        final LiveData<Resource<List<LanguageModel>>> source =
                getDownloadedLanguagesUseCase.execute();

        source.observeForever(new Observer<Resource<List<LanguageModel>>>() {
            @Override
            public void onChanged(Resource<List<LanguageModel>> resource) {
                if (resource == null) return;
                languageListLiveData.postValue(resource);

                if (resource.isSuccess() || resource.isError()) {
                    source.removeObserver(this);
                }
            }
        });
    }

    /**
     * Initiates the download of the ML Kit translation model for the
     * specified language.
     *
     * <p>Sets the active language code via {@link #getActiveLanguageCodeLiveData()}
     * to trigger a per-item loading indicator. Reports real byte-level progress
     * via {@link #getDownloadProgressLiveData()}. Clears both and refreshes the
     * language list when the download completes.</p>
     *
     * @param languageCode the BCP-47 code of the language model to download (e.g. "id")
     * @param requireWifi  if {@code true}, download only proceeds on Wi-Fi
     */
    public void downloadModel(@NonNull String languageCode, boolean requireWifi) {
        FileLogger.d(TAG, "Download model requested: " + languageCode
                + " (requireWifi=" + requireWifi + ")");
        activeLanguageCodeLiveData.setValue(languageCode);
        downloadProgressLiveData.setValue(null);

        final LiveData<Resource<Object>> source =
                downloadLanguageModelUseCase.execute(languageCode, requireWifi);

        source.observeForever(new Observer<Resource<Object>>() {
            @Override
            public void onChanged(Resource<Object> resource) {
                if (resource == null) return;

                if (resource.isProgress()) {
                    // Forward byte-level progress to the UI
                    if (resource.getData() instanceof DownloadProgress) {
                        final DownloadProgress progress = (DownloadProgress) resource.getData();
                        downloadProgressLiveData.postValue(progress);
                        FileLogger.d(TAG, "Download progress [" + languageCode + "]: "
                                + progress.getPercent() + "%");
                    }
                    return; // keep observing
                }

                if (resource.isSuccess()) {
                    // getData() is the language code String on success
                    final String code = resource.getData() instanceof String
                            ? (String) resource.getData()
                            : languageCode;
                    final Resource<String> successResult = Resource.success(code);
                    downloadResultLiveData.postValue(successResult);
                    source.removeObserver(this);
                    activeLanguageCodeLiveData.postValue(null);
                    downloadProgressLiveData.postValue(null);
                    loadLanguages();
                    FileLogger.i(TAG, "Model downloaded successfully: " + languageCode);

                } else if (resource.isError()) {
                    final Resource<String> errorResult =
                            Resource.error(resource.getMessage() != null
                                    ? resource.getMessage()
                                    : "Download failed.", null);
                    downloadResultLiveData.postValue(errorResult);
                    source.removeObserver(this);
                    activeLanguageCodeLiveData.postValue(null);
                    downloadProgressLiveData.postValue(null);
                    postError("Download failed: " + resource.getMessage());
                    FileLogger.e(TAG, "Download failed: " + resource.getMessage());
                }
                // LOADING state: do nothing, keep observing
            }
        });
    }

    /**
     * Deletes the downloaded ML Kit translation model for the specified language.
     *
     * <p>Sets the active language code via {@link #getActiveLanguageCodeLiveData()}
     * to trigger a per-item loading indicator. Clears the active code and
     * refreshes the language list when the deletion completes.</p>
     *
     * @param languageCode the BCP-47 code of the language model to delete (e.g. "id")
     */
    public void deleteModel(@NonNull String languageCode) {
        FileLogger.d(TAG, "Delete model requested: " + languageCode);
        activeLanguageCodeLiveData.setValue(languageCode);

        final LiveData<Resource<String>> source =
                deleteLanguageModelUseCase.execute(languageCode);

        source.observeForever(new Observer<Resource<String>>() {
            @Override
            public void onChanged(Resource<String> resource) {
                if (resource == null) return;
                deleteResultLiveData.postValue(resource);

                if (resource.isSuccess() || resource.isError()) {
                    source.removeObserver(this);
                    activeLanguageCodeLiveData.postValue(null);
                    if (resource.isSuccess()) {
                        loadLanguages();
                        FileLogger.i(TAG, "Model deleted successfully: " + languageCode);
                    } else {
                        postError("Delete failed: " + resource.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Clears the download result LiveData after it has been consumed by the UI.
     * Call this after showing a Snackbar to prevent re-delivery on re-subscribe.
     */
    public void clearDownloadResult() {
        downloadResultLiveData.setValue(null);
    }

    /**
     * Clears the delete result LiveData after it has been consumed by the UI.
     * Call this after showing a Snackbar to prevent re-delivery on re-subscribe.
     */
    public void clearDeleteResult() {
        deleteResultLiveData.setValue(null);
    }

    // -------------------------------------------------------------------------
    // LiveData accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link LiveData} emitting the full list of supported languages
     * with their current download status.
     *
     * @return {@link LiveData} of {@link Resource} wrapped list of {@link LanguageModel}
     */
    @NonNull
    public LiveData<Resource<List<LanguageModel>>> getLanguageListLiveData() {
        return languageListLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting the result of the most recent
     * model download operation.
     *
     * @return {@link LiveData} of {@link Resource} wrapped BCP-47 language code string
     */
    @NonNull
    public LiveData<Resource<String>> getDownloadResultLiveData() {
        return downloadResultLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting the result of the most recent
     * model deletion operation.
     *
     * @return {@link LiveData} of {@link Resource} wrapped BCP-47 language code string
     */
    @NonNull
    public LiveData<Resource<String>> getDeleteResultLiveData() {
        return deleteResultLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting the BCP-47 language code of the
     * model currently being downloaded or deleted.
     *
     * <p>Observe this in the RecyclerView adapter to show a per-item
     * progress indicator. Emits {@code null} when no operation is active.</p>
     *
     * @return {@link LiveData} of the active language code, or {@code null}
     */
    @NonNull
    public LiveData<String> getActiveLanguageCodeLiveData() {
        return activeLanguageCodeLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting real byte-level download progress
     * for the currently active model download.
     *
     * <p>Observe this to update a per-item progress bar in the RecyclerView.
     * Emits {@code null} when no download is active.</p>
     *
     * @return {@link LiveData} of {@link DownloadProgress}, or {@code null}
     */
    @NonNull
    public LiveData<DownloadProgress> getDownloadProgressLiveData() {
        return downloadProgressLiveData;
    }
}
