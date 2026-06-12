package com.igoy86.nexttranslate.domain.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.util.Resource;

/**
 * Repository interface defining the contract for AI word-explanation operations
 * powered by the NextTranslate Vercel backend (Groq + LLaMA).
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Retrofit, OkHttp, or any other networking library directly. The actual
 * implementation resides in the data layer
 * ({@link com.igoy86.nexttranslate.data.repository.ExplainRepositoryImpl}).</p>
 *
 * <p>By depending on this interface rather than its implementation, the
 * domain and presentation layers remain decoupled from networking internals,
 * making the codebase easier to test and maintain.</p>
 */
public interface ExplainRepository {

    /**
     * Requests an AI-generated explanation for the given word.
     *
     * <p>Returns a {@link LiveData} stream that emits:</p>
     * <ul>
     *     <li>{@link Resource#loading(Object)} immediately when the request starts</li>
     *     <li>{@link Resource#success(Object)} with the explanation {@link String}
     *         when the backend responds successfully</li>
     *     <li>{@link Resource#error(String, Object)} if the network request fails
     *         or the backend returns an error response</li>
     * </ul>
     *
     * @param word       the word to explain; must not be null or empty
     * @param language   the full English name of the word's language (e.g. "English")
     * @param definition an optional short definition for AI context; may be null
     * @return a {@link LiveData} emitting {@link Resource} wrapped explanation {@link String}
     */
    @NonNull
    LiveData<Resource<String>> explain(
            @NonNull String word,
            @NonNull String language,
            @Nullable String definition
    );
}
