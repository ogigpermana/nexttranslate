package com.igoy86.nexttranslate.presentation.translate;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.domain.usecase.favorite.AddFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.AddHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.DetectLanguageUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.TranslateTextUseCase;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Factory class responsible for creating instances of {@link TranslateViewModel}.
 *
 * <p>Android's {@link ViewModelProvider} requires a no-argument constructor
 * to instantiate ViewModels by default. Since {@link TranslateViewModel}
 * requires multiple use case dependencies, this factory provides them during
 * construction while still integrating with the {@link ViewModelProvider} API.</p>
 *
 * <p>This factory is instantiated with dependencies sourced from
 * {@link com.igoy86.nexttranslate.di.AppContainer}, accessed via
 * {@link com.igoy86.nexttranslate.NextTranslateApp#getContainer()}.</p>
 *
 * <p>Usage example in a Fragment:</p>
 * <pre>
 *     AppContainer container = NextTranslateApp.getContainer();
 *
 *     TranslateViewModelFactory factory = new TranslateViewModelFactory(
 *             container.getTranslateTextUseCase(),
 *             container.getDetectLanguageUseCase(),
 *             container.getAddHistoryUseCase(),
 *             container.getAddFavoriteUseCase()
 *     );
 *
 *     TranslateViewModel viewModel = new ViewModelProvider(this, factory)
 *             .get(TranslateViewModel.class);
 * </pre>
 */
public class TranslateViewModelFactory implements ViewModelProvider.Factory {

    /** Tag used for logging events originating from this factory. */
    private static final String TAG = "TranslateVMFactory";

    /** Use case for performing ML Kit text translation. */
    @NonNull
    private final TranslateTextUseCase translateTextUseCase;

    /** Use case for detecting the language of the input text. */
    @NonNull
    private final DetectLanguageUseCase detectLanguageUseCase;

    /** Use case for persisting a completed translation to history. */
    @NonNull
    private final AddHistoryUseCase addHistoryUseCase;

    /** Use case for bookmarking a translation as a favorite. */
    @NonNull
    private final AddFavoriteUseCase addFavoriteUseCase;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link TranslateViewModelFactory} with all required
     * use case dependencies.
     *
     * @param translateTextUseCase  use case for text translation; must not be null
     * @param detectLanguageUseCase use case for language detection; must not be null
     * @param addHistoryUseCase     use case for saving to history; must not be null
     * @param addFavoriteUseCase    use case for adding to favorites; must not be null
     */
    public TranslateViewModelFactory(
            @NonNull TranslateTextUseCase translateTextUseCase,
            @NonNull DetectLanguageUseCase detectLanguageUseCase,
            @NonNull AddHistoryUseCase addHistoryUseCase,
            @NonNull AddFavoriteUseCase addFavoriteUseCase
    ) {
        this.translateTextUseCase = translateTextUseCase;
        this.detectLanguageUseCase = detectLanguageUseCase;
        this.addHistoryUseCase = addHistoryUseCase;
        this.addFavoriteUseCase = addFavoriteUseCase;
    }

    // -------------------------------------------------------------------------
    // ViewModelProvider.Factory implementation
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance of the requested {@link ViewModel} class.
     *
     * <p>Only {@link TranslateViewModel} is supported by this factory.
     * Requesting any other ViewModel class will throw an
     * {@link IllegalArgumentException}.</p>
     *
     * @param modelClass the class of the ViewModel to create; must not be null
     * @param <T>        the type of the ViewModel
     * @return a newly created {@link TranslateViewModel} instance
     * @throws IllegalArgumentException if {@code modelClass} is not
     *                                  {@link TranslateViewModel}
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TranslateViewModel.class)) {
            FileLogger.d(TAG, "Creating TranslateViewModel instance.");

            //noinspection unchecked
            return (T) new TranslateViewModel(
                    translateTextUseCase,
                    detectLanguageUseCase,
                    addHistoryUseCase,
                    addFavoriteUseCase
            );
        }
        throw new IllegalArgumentException(
                "TranslateViewModelFactory cannot create an instance of: "
                        + modelClass.getName()
        );
    }
}