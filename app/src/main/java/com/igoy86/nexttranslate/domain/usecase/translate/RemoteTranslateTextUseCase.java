package com.igoy86.nexttranslate.domain.usecase.translate;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.domain.repository.RemoteTranslateRepository;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Use case responsible for performing text translation via the
 * NextTranslate Vercel backend (Groq + LLaMA).
 *
 * <p>Encapsulates the remote translation business logic, delegating the
 * actual network call to {@link RemoteTranslateRepository}.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link RemoteTranslateRepository} interface, not its implementation,
 * keeping it decoupled from Retrofit or any networking library.</p>
 *
 * <p>Language names passed to {@link #execute} should be full English names
 * (e.g. {@code "Indonesian"}, {@code "English"}) rather than BCP-47 codes,
 * because the Groq LLM backend uses them directly inside its translation prompt.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     RemoteTranslateTextUseCase useCase = container.getRemoteTranslateTextUseCase();
 *     useCase.execute("Halo dunia", "Indonesian", "English").observe(this, resource -> {
 *         if (resource.isSuccess()) {
 *             String result = resource.getData().getTranslatedText();
 *         }
 *     });
 * </pre>
 */
public class RemoteTranslateTextUseCase {

    /** Repository used to perform the remote translation operation. */
    @NonNull
    private final RemoteTranslateRepository remoteTranslateRepository;

    /**
     * Constructs a new {@link RemoteTranslateTextUseCase} with the given repository.
     *
     * @param remoteTranslateRepository the repository used to perform remote translation;
     *                                  must not be null
     */
    public RemoteTranslateTextUseCase(@NonNull RemoteTranslateRepository remoteTranslateRepository) {
        this.remoteTranslateRepository = remoteTranslateRepository;
    }

    /**
     * Executes the use case by translating the given source text via the remote backend.
     *
     * @param sourceText the text to translate; must not be null or empty
     * @param sourceLang the full English name of the source language (e.g. "Indonesian")
     * @param targetLang the full English name of the target language (e.g. "Japanese")
     * @return a {@link LiveData} emitting {@link Resource} wrapped {@link TranslationResult}
     */
    @NonNull
    public LiveData<Resource<TranslationResult>> execute(
            @NonNull String sourceText,
            @NonNull String sourceLang,
            @NonNull String targetLang
    ) {
        return remoteTranslateRepository.translate(sourceText, sourceLang, targetLang);
    }
}
