package com.igoy86.nexttranslate.domain.repository;

import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.DownloadProgress;
import com.igoy86.nexttranslate.domain.model.LanguageModel;
import com.igoy86.nexttranslate.util.Resource;

import java.util.List;

/**
 * Repository interface defining the contract for ML Kit language model
 * management operations.
 */
public interface LanguageModelRepository {

    /**
     * Downloads the ML Kit translation model for the specified language.
     *
     * <p>Returns a {@link LiveData} stream that emits:</p>
     * <ul>
     *     <li>{@link Resource#loading(Object)} — download started</li>
     *     <li>{@link Resource#progress(Object)} with {@link DownloadProgress} — bytes downloaded so far</li>
     *     <li>{@link Resource#success(Object)} with the language code — download complete</li>
     *     <li>{@link Resource#error(String, Object)} — download failed</li>
     * </ul>
     *
     * @param languageCode the BCP-47 language code (e.g. "en", "id")
     * @param requireWifi  if {@code true}, download only on Wi-Fi
     * @return a {@link LiveData} emitting {@link Resource} of {@link DownloadProgress} or language code string
     */
    LiveData<Resource<Object>> downloadModel(String languageCode, boolean requireWifi);

    /**
     * Deletes the ML Kit translation model for the specified language.
     *
     * @param languageCode the BCP-47 language code (e.g. "en", "id")
     * @return a {@link LiveData} emitting {@link Resource} wrapped language code string
     */
    LiveData<Resource<String>> deleteModel(String languageCode);

    /**
     * Retrieves the list of all supported languages with their download status.
     *
     * @return a {@link LiveData} emitting {@link Resource} wrapped list of {@link LanguageModel}
     */
    LiveData<Resource<List<LanguageModel>>> getDownloadedLanguages();
}
