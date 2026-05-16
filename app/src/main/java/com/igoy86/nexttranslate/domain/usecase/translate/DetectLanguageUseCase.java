package com.igoy86.nexttranslate.domain.usecase.translate;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.repository.TranslateRepository;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Use case responsible for detecting the language of a given text
 * using ML Kit Language Identification.
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link TranslateRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     DetectLanguageUseCase detectLanguageUseCase = container.getDetectLanguageUseCase();
 *     detectLanguageUseCase.execute("Halo dunia").observe(this, resource -> {
 *         if (resource.isSuccess()) {
 *             String detectedCode = resource.getData(); // "id"
 *         }
 *     });
 * </pre>
 */
public class DetectLanguageUseCase {

    /** Repository used to perform language detection. */
    @NonNull
    private final TranslateRepository translateRepository;

    /**
     * Constructs a new {@link DetectLanguageUseCase} with the given repository.
     *
     * @param translateRepository the repository used to detect language
     */
    public DetectLanguageUseCase(@NonNull TranslateRepository translateRepository) {
        this.translateRepository = translateRepository;
    }

    /**
     * Executes the use case by detecting the language of the given text.
     *
     * @param text the text whose language is to be detected; must not be null or empty
     * @return a {@link LiveData} emitting {@link Resource} wrapped BCP-47 language code string
     */
    @NonNull
    public LiveData<Resource<String>> execute(@NonNull String text) {
        return translateRepository.detectLanguage(text);
    }
}