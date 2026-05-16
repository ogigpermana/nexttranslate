package com.igoy86.nexttranslate.domain.usecase.translate;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.domain.repository.TranslateRepository;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Use case responsible for performing text translation via ML Kit Translate.
 *
 * <p>Encapsulates the core translation business logic, delegating the actual
 * ML Kit operation to {@link TranslateRepository}.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link TranslateRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     TranslateTextUseCase translateTextUseCase = container.getTranslateTextUseCase();
 *     translateTextUseCase.execute("Hello", "en", "id").observe(this, resource -> {
 *         if (resource.isSuccess()) {
 *             String result = resource.getData().getTranslatedText();
 *         }
 *     });
 * </pre>
 */
public class TranslateTextUseCase {

    /** Repository used to perform the translation operation. */
    @NonNull
    private final TranslateRepository translateRepository;

    /**
     * Constructs a new {@link TranslateTextUseCase} with the given repository.
     *
     * @param translateRepository the repository used to perform translation
     */
    public TranslateTextUseCase(@NonNull TranslateRepository translateRepository) {
        this.translateRepository = translateRepository;
    }

    /**
     * Executes the use case by translating the given source text.
     *
     * @param sourceText         the text to translate; must not be null or empty
     * @param sourceLanguageCode the BCP-47 code of the source language (e.g. "en")
     * @param targetLanguageCode the BCP-47 code of the target language (e.g. "id")
     * @return a {@link LiveData} emitting {@link Resource} wrapped {@link TranslationResult}
     */
    @NonNull
    public LiveData<Resource<TranslationResult>> execute(
            @NonNull String sourceText,
            @NonNull String sourceLanguageCode,
            @NonNull String targetLanguageCode
    ) {
        return translateRepository.translate(sourceText, sourceLanguageCode, targetLanguageCode);
    }
}