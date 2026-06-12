package com.igoy86.nexttranslate.domain.repository;

import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Repository interface defining the contract for remote translation operations
 * powered by the NextTranslate Vercel backend (Groq + LLaMA).
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Retrofit, OkHttp, or any other networking library directly. The actual
 * implementation resides in the data layer
 * ({@link com.igoy86.nexttranslate.data.repository.RemoteTranslateRepositoryImpl}).</p>
 *
 * <p>By depending on this interface rather than its implementation, the
 * domain and presentation layers remain decoupled from networking internals,
 * making the codebase easier to test and maintain.</p>
 *
 * <p>Language names passed to this repository should be full English names
 * (e.g. {@code "Indonesian"}, {@code "English"}) rather than BCP-47 codes,
 * because the Groq LLM backend uses them directly inside its prompt for
 * better translation accuracy.</p>
 */
public interface RemoteTranslateRepository {

    /**
     * Translates the given source text from the specified source language
     * to the specified target language using the Groq backend.
     *
     * <p>Returns a {@link LiveData} stream that emits:</p>
     * <ul>
     *     <li>{@link Resource#loading(Object)} immediately when the request starts</li>
     *     <li>{@link Resource#success(Object)} with the {@link TranslationResult}
     *         when the backend responds successfully</li>
     *     <li>{@link Resource#error(String, Object)} if the network request fails
     *         or the backend returns an error response</li>
     * </ul>
     *
     * @param sourceText the text to translate; must not be null or empty
     * @param sourceLang the full English name of the source language (e.g. "Indonesian")
     * @param targetLang the full English name of the target language (e.g. "Japanese")
     * @return a {@link LiveData} emitting {@link Resource} wrapped {@link TranslationResult}
     */
    LiveData<Resource<TranslationResult>> translate(
            String sourceText,
            String sourceLang,
            String targetLang
    );
}
