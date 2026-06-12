package com.igoy86.nexttranslate.domain.usecase.dictionary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.repository.ExplainRepository;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Use case responsible for requesting an AI-generated explanation
 * for a word via the NextTranslate Vercel backend (Groq + LLaMA).
 *
 * <p>Encapsulates the explain business logic, delegating the actual network
 * call to {@link ExplainRepository}.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link ExplainRepository} interface, not its implementation,
 * keeping it decoupled from Retrofit or any networking library.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     ExplainWordUseCase useCase = container.getExplainWordUseCase();
 *     useCase.execute("serendipity", "English", "a happy accident").observe(this, resource -> {
 *         if (resource.isSuccess()) {
 *             String explanation = resource.getData();
 *         }
 *     });
 * </pre>
 */
public class ExplainWordUseCase {

    /** Repository used to perform the remote explain operation. */
    @NonNull
    private final ExplainRepository explainRepository;

    /**
     * Constructs a new {@link ExplainWordUseCase} with the given repository.
     *
     * @param explainRepository the repository used to perform word explanation;
     *                          must not be null
     */
    public ExplainWordUseCase(@NonNull ExplainRepository explainRepository) {
        this.explainRepository = explainRepository;
    }

    /**
     * Executes the use case by requesting an AI explanation for the given word.
     *
     * @param word       the word to explain; must not be null or empty
     * @param language   the full English name of the word's language (e.g. "English")
     * @param definition an optional short definition for AI context; may be null
     * @return a {@link LiveData} emitting {@link Resource} wrapped explanation {@link String}
     */
    @NonNull
    public LiveData<Resource<String>> execute(
            @NonNull String word,
            @NonNull String language,
            @Nullable String definition
    ) {
        return explainRepository.explain(word, language, definition);
    }
}
