package com.igoy86.nexttranslate.domain.usecase.language;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.repository.LanguageModelRepository;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Use case responsible for downloading an ML Kit translation model
 * for a specific language.
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link LanguageModelRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     DownloadLanguageModelUseCase downloadUseCase = container.getDownloadLanguageModelUseCase();
 *     downloadUseCase.execute("id", false).observe(this, resource -> {
 *         if (resource.isLoading())   showSpinner();
 *         if (resource.isProgress())  updateProgressBar((DownloadProgress) resource.getData());
 *         if (resource.isSuccess())   hideProgressBar();
 *         if (resource.isError())     showError(resource.getMessage());
 *     });
 * </pre>
 */
public class DownloadLanguageModelUseCase {

    /** Repository used to download the language model. */
    @NonNull
    private final LanguageModelRepository languageModelRepository;

    /**
     * Constructs a new {@link DownloadLanguageModelUseCase} with the given repository.
     *
     * @param languageModelRepository the repository used to download language models
     */
    public DownloadLanguageModelUseCase(@NonNull LanguageModelRepository languageModelRepository) {
        this.languageModelRepository = languageModelRepository;
    }

    /**
     * Executes the use case by initiating the download of the ML Kit
     * translation model for the specified language.
     *
     * <p>The returned {@link LiveData} emits:</p>
     * <ul>
     *     <li>{@link Resource#loading} — download started</li>
     *     <li>{@link Resource#progress} with {@link com.igoy86.nexttranslate.domain.model.DownloadProgress} — repeated as bytes arrive</li>
     *     <li>{@link Resource#success} with the language code — download complete</li>
     *     <li>{@link Resource#error} — download failed</li>
     * </ul>
     *
     * @param languageCode the BCP-47 language code of the model to download (e.g. "id", "ar")
     * @param requireWifi  if {@code true}, download only proceeds over Wi-Fi
     * @return a {@link LiveData} emitting {@link Resource} of {@link Object}
     *         (either {@link com.igoy86.nexttranslate.domain.model.DownloadProgress} or language code String)
     */
    @NonNull
    public LiveData<Resource<Object>> execute(@NonNull String languageCode, boolean requireWifi) {
        return languageModelRepository.downloadModel(languageCode, requireWifi);
    }
}
