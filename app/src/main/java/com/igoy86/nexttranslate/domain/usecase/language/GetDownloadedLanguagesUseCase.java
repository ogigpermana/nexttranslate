package com.igoy86.nexttranslate.domain.usecase.language;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.LanguageModel;
import com.igoy86.nexttranslate.domain.repository.LanguageModelRepository;
import com.igoy86.nexttranslate.util.Resource;

import java.util.List;

/**
 * Use case responsible for retrieving the list of all supported languages
 * along with their current download status on the device.
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link LanguageModelRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     GetDownloadedLanguagesUseCase getLanguagesUseCase = container.getGetDownloadedLanguagesUseCase();
 *     getLanguagesUseCase.execute().observe(this, resource -> {
 *         if (resource.isSuccess()) {
 *             List{@literal <}LanguageModel{@literal >} languages = resource.getData();
 *             adapter.submitList(languages);
 *         }
 *     });
 * </pre>
 */
public class GetDownloadedLanguagesUseCase {

    /** Repository used to retrieve language model download status. */
    @NonNull
    private final LanguageModelRepository languageModelRepository;

    /**
     * Constructs a new {@link GetDownloadedLanguagesUseCase} with the given repository.
     *
     * @param languageModelRepository the repository used to query language models
     */
    public GetDownloadedLanguagesUseCase(@NonNull LanguageModelRepository languageModelRepository) {
        this.languageModelRepository = languageModelRepository;
    }

    /**
     * Executes the use case by retrieving all supported languages and
     * their current download status.
     *
     * @return a {@link LiveData} emitting {@link Resource} wrapped list of {@link LanguageModel}
     */
    @NonNull
    public LiveData<Resource<List<LanguageModel>>> execute() {
        return languageModelRepository.getDownloadedLanguages();
    }
}