package com.igoy86.nexttranslate.presentation.language;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.domain.usecase.language.DeleteLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.DownloadLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.GetDownloadedLanguagesUseCase;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Factory class responsible for creating instances of {@link LanguageViewModel}.
 *
 * <p>Provides the required use case dependencies to {@link LanguageViewModel}
 * while integrating with Android's {@link ViewModelProvider} API.</p>
 *
 * <p>Usage example in a Fragment:</p>
 * <pre>
 *     AppContainer container = NextTranslateApp.getContainer();
 *
 *     LanguageViewModelFactory factory = new LanguageViewModelFactory(
 *             container.getGetDownloadedLanguagesUseCase(),
 *             container.getDownloadLanguageModelUseCase(),
 *             container.getDeleteLanguageModelUseCase()
 *     );
 *
 *     LanguageViewModel viewModel = new ViewModelProvider(this, factory)
 *             .get(LanguageViewModel.class);
 * </pre>
 */
public class LanguageViewModelFactory implements ViewModelProvider.Factory {

    /** Tag used for logging events originating from this factory. */
    private static final String TAG = "LanguageVMFactory";

    /** Use case for retrieving all supported languages with download status. */
    @NonNull
    private final GetDownloadedLanguagesUseCase getDownloadedLanguagesUseCase;

    /** Use case for downloading an ML Kit translation model. */
    @NonNull
    private final DownloadLanguageModelUseCase downloadLanguageModelUseCase;

    /** Use case for deleting a downloaded ML Kit translation model. */
    @NonNull
    private final DeleteLanguageModelUseCase deleteLanguageModelUseCase;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link LanguageViewModelFactory} with all required
     * use case dependencies.
     *
     * @param getDownloadedLanguagesUseCase use case for retrieving languages; must not be null
     * @param downloadLanguageModelUseCase  use case for downloading models; must not be null
     * @param deleteLanguageModelUseCase    use case for deleting models; must not be null
     */
    public LanguageViewModelFactory(
            @NonNull GetDownloadedLanguagesUseCase getDownloadedLanguagesUseCase,
            @NonNull DownloadLanguageModelUseCase downloadLanguageModelUseCase,
            @NonNull DeleteLanguageModelUseCase deleteLanguageModelUseCase
    ) {
        this.getDownloadedLanguagesUseCase = getDownloadedLanguagesUseCase;
        this.downloadLanguageModelUseCase = downloadLanguageModelUseCase;
        this.deleteLanguageModelUseCase = deleteLanguageModelUseCase;
    }

    // -------------------------------------------------------------------------
    // ViewModelProvider.Factory implementation
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance of the requested {@link ViewModel} class.
     *
     * <p>Only {@link LanguageViewModel} is supported by this factory.
     * Requesting any other ViewModel class will throw an
     * {@link IllegalArgumentException}.</p>
     *
     * @param modelClass the class of the ViewModel to create; must not be null
     * @param <T>        the type of the ViewModel
     * @return a newly created {@link LanguageViewModel} instance
     * @throws IllegalArgumentException if {@code modelClass} is not
     *                                  {@link LanguageViewModel}
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LanguageViewModel.class)) {
            FileLogger.d(TAG, "Creating LanguageViewModel instance.");

            //noinspection unchecked
            return (T) new LanguageViewModel(
                    getDownloadedLanguagesUseCase,
                    downloadLanguageModelUseCase,
                    deleteLanguageModelUseCase
            );
        }
        throw new IllegalArgumentException(
                "LanguageViewModelFactory cannot create an instance of: "
                        + modelClass.getName()
        );
    }
}