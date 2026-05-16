package com.igoy86.nexttranslate.domain.repository;

import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Repository interface defining the contract for translation operations
 * powered by ML Kit Translate.
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * any Android framework or ML Kit classes directly. The actual implementation
 * resides in the data layer ({@code TranslateRepositoryImpl}).</p>
 *
 * <p>By depending on this interface rather than its implementation, the
 * domain and presentation layers remain decoupled from ML Kit internals,
 * making the codebase easier to test and maintain.</p>
 */
public interface TranslateRepository {

    /**
     * Translates the given source text from the specified source language
     * to the specified target language using ML Kit Translate.
     *
     * <p>Returns a {@link LiveData} stream that emits:</p>
     * <ul>
     *     <li>{@link Resource#loading(Object)} immediately when the operation starts</li>
     *     <li>{@link Resource#success(Object)} with the {@link TranslationResult}
     *         when translation completes</li>
     *     <li>{@link Resource#error(String, Object)} if the operation fails,
     *         for example when the language model is not downloaded</li>
     * </ul>
     *
     * @param sourceText         the text to translate; must not be null or empty
     * @param sourceLanguageCode the BCP-47 code of the source language (e.g. "en")
     * @param targetLanguageCode the BCP-47 code of the target language (e.g. "id")
     * @return a {@link LiveData} emitting {@link Resource} wrapped {@link TranslationResult}
     */
    LiveData<Resource<TranslationResult>> translate(
            String sourceText,
            String sourceLanguageCode,
            String targetLanguageCode
    );

    /**
     * Detects the language of the given text using ML Kit Language Identification.
     *
     * <p>Returns a {@link LiveData} stream that emits:</p>
     * <ul>
     *     <li>{@link Resource#loading(Object)} immediately when detection starts</li>
     *     <li>{@link Resource#success(Object)} with the detected BCP-47 language
     *         code string (e.g. "en", "id") when detection succeeds</li>
     *     <li>{@link Resource#error(String, Object)} if detection fails or
     *         the language cannot be identified with sufficient confidence</li>
     * </ul>
     *
     * @param text the text whose language is to be detected; must not be null or empty
     * @return a {@link LiveData} emitting {@link Resource} wrapped BCP-47 language code string
     */
    LiveData<Resource<String>> detectLanguage(String text);
}