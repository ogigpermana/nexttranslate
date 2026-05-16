package com.igoy86.nexttranslate.domain.usecase.language;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.repository.LanguageModelRepository;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Use case responsible for deleting a downloaded ML Kit translation model
 * from the device storage to free up space.
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link LanguageModelRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     DeleteLanguageModelUseCase deleteModelUseCase = container.getDeleteLanguageModelUseCase();
 *     deleteModelUseCase.execute("id").observe(this, resource -> {
 *         if (resource.isSuccess()) refreshLanguageList();
 *     });
 * </pre>
 */
public class DeleteLanguageModelUseCase {

    /** Repository used to delete the language model. */
    @NonNull
    private final LanguageModelRepository languageModelRepository;

    /**
     * Constructs a new {@link DeleteLanguageModelUseCase} with the given repository.
     *
     * @param languageModelRepository the repository used to delete language models
     */
    public DeleteLanguageModelUseCase(@NonNull LanguageModelRepository languageModelRepository) {
        this.languageModelRepository = languageModelRepository;
    }

    /**
     * Executes the use case by deleting the ML Kit translation model
     * for the specified language from device storage.
     *
     * @param languageCode the BCP-47 language code of the model to delete (e.g. "id", "ar")
     * @return a {@link LiveData} emitting {@link Resource} wrapped language code string
     */
    @NonNull
    public LiveData<Resource<String>> execute(@NonNull String languageCode) {
        return languageModelRepository.deleteModel(languageCode);
    }
}