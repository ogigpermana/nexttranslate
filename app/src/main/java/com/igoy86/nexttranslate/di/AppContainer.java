package com.igoy86.nexttranslate.di;

import android.content.Context;

import com.igoy86.nexttranslate.data.local.database.AppDatabase;
import com.igoy86.nexttranslate.data.local.dao.HistoryDao;
import com.igoy86.nexttranslate.data.local.dao.FavoriteDao;
import com.igoy86.nexttranslate.data.repository.HistoryRepositoryImpl;
import com.igoy86.nexttranslate.data.repository.FavoriteRepositoryImpl;
import com.igoy86.nexttranslate.data.repository.TranslateRepositoryImpl;
import com.igoy86.nexttranslate.data.repository.LanguageModelRepositoryImpl;
import com.igoy86.nexttranslate.domain.repository.HistoryRepository;
import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;
import com.igoy86.nexttranslate.domain.repository.TranslateRepository;
import com.igoy86.nexttranslate.domain.repository.LanguageModelRepository;
import com.igoy86.nexttranslate.domain.usecase.history.AddHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.GetAllHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.DeleteHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.ClearAllHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.AddFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.GetAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.DeleteFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.TranslateTextUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.DetectLanguageUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.DownloadLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.DeleteLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.GetDownloadedLanguagesUseCase;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Manual dependency injection container for the NextTranslate application.
 *
 * <p>Acts as a service locator that constructs and holds all application-level
 * dependencies. This eliminates the need for a third-party DI framework such as
 * Hilt or Dagger, keeping the project lightweight and easy to understand for
 * buyers on CodeCanyon.</p>
 *
 * <p>The container is instantiated once in {@link com.igoy86.nexttranslate.NextTranslateApp}
 * and accessed globally via {@code NextTranslateApp.getContainer()}.</p>
 *
 * <p>Dependency graph (top to bottom):</p>
 * <pre>
 *     AppDatabase
 *         └── HistoryDao         → HistoryRepositoryImpl     → History Use Cases
 *         └── FavoriteDao        → FavoriteRepositoryImpl    → Favorite Use Cases
 *     ML Kit Translator          → TranslateRepositoryImpl   → Translate Use Cases
 *     ML Kit LanguageIdentifier  → LanguageModelRepositoryImpl → Language Use Cases
 * </pre>
 *
 * <p>Usage example in a ViewModel Factory:</p>
 * <pre>
 *     AppContainer container = NextTranslateApp.getContainer();
 *     TranslateTextUseCase translateUseCase = container.getTranslateTextUseCase();
 * </pre>
 */
public class AppContainer {

    /** Tag used for logging events originating from this container. */
    private static final String TAG = "AppContainer";

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    /** Executor pools for background and main-thread operations. */
    private final AppExecutors appExecutors;

    /** Room database instance. Created lazily and shared across DAOs. */
    private AppDatabase appDatabase;

    // -------------------------------------------------------------------------
    // DAOs
    // -------------------------------------------------------------------------

    /** Data Access Object for translation history entries. */
    private HistoryDao historyDao;

    /** Data Access Object for favorite translation entries. */
    private FavoriteDao favoriteDao;

    // -------------------------------------------------------------------------
    // Repositories
    // -------------------------------------------------------------------------

    /** Repository handling translation history persistence. */
    private HistoryRepository historyRepository;

    /** Repository handling favorite translations persistence. */
    private FavoriteRepository favoriteRepository;

    /** Repository handling ML Kit translation operations. */
    private TranslateRepository translateRepository;

    /** Repository handling ML Kit language model downloads and deletions. */
    private LanguageModelRepository languageModelRepository;

    // -------------------------------------------------------------------------
    // Use Cases — History
    // -------------------------------------------------------------------------

    /** Use case for adding a new entry to translation history. */
    private AddHistoryUseCase addHistoryUseCase;

    /** Use case for retrieving all translation history entries. */
    private GetAllHistoryUseCase getAllHistoryUseCase;

    /** Use case for deleting a single translation history entry. */
    private DeleteHistoryUseCase deleteHistoryUseCase;

    /** Use case for clearing all translation history entries. */
    private ClearAllHistoryUseCase clearAllHistoryUseCase;

    // -------------------------------------------------------------------------
    // Use Cases — Favorite
    // -------------------------------------------------------------------------

    /** Use case for adding a translation to favorites. */
    private AddFavoriteUseCase addFavoriteUseCase;

    /** Use case for retrieving all favorite translations. */
    private GetAllFavoritesUseCase getAllFavoritesUseCase;

    /** Use case for deleting a favorite translation. */
    private DeleteFavoriteUseCase deleteFavoriteUseCase;

    // -------------------------------------------------------------------------
    // Use Cases — Translate
    // -------------------------------------------------------------------------

    /** Use case for performing text translation via ML Kit. */
    private TranslateTextUseCase translateTextUseCase;

    /** Use case for detecting the language of a given text. */
    private DetectLanguageUseCase detectLanguageUseCase;

    // -------------------------------------------------------------------------
    // Use Cases — Language Model
    // -------------------------------------------------------------------------

    /** Use case for downloading an ML Kit language translation model. */
    private DownloadLanguageModelUseCase downloadLanguageModelUseCase;

    /** Use case for deleting a downloaded ML Kit language translation model. */
    private DeleteLanguageModelUseCase deleteLanguageModelUseCase;

    /** Use case for retrieving all currently downloaded language models. */
    private GetDownloadedLanguagesUseCase getDownloadedLanguagesUseCase;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link AppContainer} with the given application context.
     *
     * <p>All dependencies are initialized lazily on first access to avoid
     * unnecessary resource allocation at startup.</p>
     *
     * @param context the application {@link Context} used to build the database
     *                and initialize ML Kit components
     */
    public AppContainer(Context context) {
        this.appExecutors = AppExecutors.getInstance();
        initDatabase(context);
        FileLogger.d(TAG, "AppContainer initialized.");
    }

    // -------------------------------------------------------------------------
    // Private initialization helpers
    // -------------------------------------------------------------------------

    /**
     * Initializes the Room database and retrieves all DAOs.
     *
     * @param context the application {@link Context}
     */
    private void initDatabase(Context context) {
        appDatabase = AppDatabase.getInstance(context);
        historyDao = appDatabase.historyDao();
        favoriteDao = appDatabase.favoriteDao();
        FileLogger.d(TAG, "Database and DAOs initialized.");
    }

    /**
     * Lazily initializes and returns the {@link HistoryRepository}.
     *
     * @return the singleton {@link HistoryRepository} instance
     */
    private HistoryRepository provideHistoryRepository() {
        if (historyRepository == null) {
            historyRepository = new HistoryRepositoryImpl(historyDao, appExecutors);
            FileLogger.d(TAG, "HistoryRepository created.");
        }
        return historyRepository;
    }

    /**
     * Lazily initializes and returns the {@link FavoriteRepository}.
     *
     * @return the singleton {@link FavoriteRepository} instance
     */
    private FavoriteRepository provideFavoriteRepository() {
        if (favoriteRepository == null) {
            favoriteRepository = new FavoriteRepositoryImpl(favoriteDao, appExecutors);
            FileLogger.d(TAG, "FavoriteRepository created.");
        }
        return favoriteRepository;
    }

    /**
     * Lazily initializes and returns the {@link TranslateRepository}.
     *
     * @return the singleton {@link TranslateRepository} instance
     */
    private TranslateRepository provideTranslateRepository() {
        if (translateRepository == null) {
            translateRepository = new TranslateRepositoryImpl(appExecutors);
            FileLogger.d(TAG, "TranslateRepository created.");
        }
        return translateRepository;
    }

    /**
     * Lazily initializes and returns the {@link LanguageModelRepository}.
     *
     * @return the singleton {@link LanguageModelRepository} instance
     */
    private LanguageModelRepository provideLanguageModelRepository() {
        if (languageModelRepository == null) {
            languageModelRepository = new LanguageModelRepositoryImpl(appExecutors);
            FileLogger.d(TAG, "LanguageModelRepository created.");
        }
        return languageModelRepository;
    }

    // -------------------------------------------------------------------------
    // Public Use Case accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link AddHistoryUseCase} instance.
     *
     * @return lazily initialized {@link AddHistoryUseCase}
     */
    public AddHistoryUseCase getAddHistoryUseCase() {
        if (addHistoryUseCase == null) {
            addHistoryUseCase = new AddHistoryUseCase(provideHistoryRepository());
        }
        return addHistoryUseCase;
    }

    /**
     * Returns the {@link GetAllHistoryUseCase} instance.
     *
     * @return lazily initialized {@link GetAllHistoryUseCase}
     */
    public GetAllHistoryUseCase getGetAllHistoryUseCase() {
        if (getAllHistoryUseCase == null) {
            getAllHistoryUseCase = new GetAllHistoryUseCase(provideHistoryRepository());
        }
        return getAllHistoryUseCase;
    }

    /**
     * Returns the {@link DeleteHistoryUseCase} instance.
     *
     * @return lazily initialized {@link DeleteHistoryUseCase}
     */
    public DeleteHistoryUseCase getDeleteHistoryUseCase() {
        if (deleteHistoryUseCase == null) {
            deleteHistoryUseCase = new DeleteHistoryUseCase(provideHistoryRepository());
        }
        return deleteHistoryUseCase;
    }

    /**
     * Returns the {@link ClearAllHistoryUseCase} instance.
     *
     * @return lazily initialized {@link ClearAllHistoryUseCase}
     */
    public ClearAllHistoryUseCase getClearAllHistoryUseCase() {
        if (clearAllHistoryUseCase == null) {
            clearAllHistoryUseCase = new ClearAllHistoryUseCase(provideHistoryRepository());
        }
        return clearAllHistoryUseCase;
    }

    /**
     * Returns the {@link AddFavoriteUseCase} instance.
     *
     * @return lazily initialized {@link AddFavoriteUseCase}
     */
    public AddFavoriteUseCase getAddFavoriteUseCase() {
        if (addFavoriteUseCase == null) {
            addFavoriteUseCase = new AddFavoriteUseCase(provideFavoriteRepository());
        }
        return addFavoriteUseCase;
    }

    /**
     * Returns the {@link GetAllFavoritesUseCase} instance.
     *
     * @return lazily initialized {@link GetAllFavoritesUseCase}
     */
    public GetAllFavoritesUseCase getGetAllFavoritesUseCase() {
        if (getAllFavoritesUseCase == null) {
            getAllFavoritesUseCase = new GetAllFavoritesUseCase(provideFavoriteRepository());
        }
        return getAllFavoritesUseCase;
    }

    /**
     * Returns the {@link DeleteFavoriteUseCase} instance.
     *
     * @return lazily initialized {@link DeleteFavoriteUseCase}
     */
    public DeleteFavoriteUseCase getDeleteFavoriteUseCase() {
        if (deleteFavoriteUseCase == null) {
            deleteFavoriteUseCase = new DeleteFavoriteUseCase(provideFavoriteRepository());
        }
        return deleteFavoriteUseCase;
    }

    /**
     * Returns the {@link TranslateTextUseCase} instance.
     *
     * @return lazily initialized {@link TranslateTextUseCase}
     */
    public TranslateTextUseCase getTranslateTextUseCase() {
        if (translateTextUseCase == null) {
            translateTextUseCase = new TranslateTextUseCase(provideTranslateRepository());
        }
        return translateTextUseCase;
    }

    /**
     * Returns the {@link DetectLanguageUseCase} instance.
     *
     * @return lazily initialized {@link DetectLanguageUseCase}
     */
    public DetectLanguageUseCase getDetectLanguageUseCase() {
        if (detectLanguageUseCase == null) {
            detectLanguageUseCase = new DetectLanguageUseCase(provideTranslateRepository());
        }
        return detectLanguageUseCase;
    }

    /**
     * Returns the {@link DownloadLanguageModelUseCase} instance.
     *
     * @return lazily initialized {@link DownloadLanguageModelUseCase}
     */
    public DownloadLanguageModelUseCase getDownloadLanguageModelUseCase() {
        if (downloadLanguageModelUseCase == null) {
            downloadLanguageModelUseCase = new DownloadLanguageModelUseCase(
                    provideLanguageModelRepository()
            );
        }
        return downloadLanguageModelUseCase;
    }

    /**
     * Returns the {@link DeleteLanguageModelUseCase} instance.
     *
     * @return lazily initialized {@link DeleteLanguageModelUseCase}
     */
    public DeleteLanguageModelUseCase getDeleteLanguageModelUseCase() {
        if (deleteLanguageModelUseCase == null) {
            deleteLanguageModelUseCase = new DeleteLanguageModelUseCase(
                    provideLanguageModelRepository()
            );
        }
        return deleteLanguageModelUseCase;
    }

    /**
     * Returns the {@link GetDownloadedLanguagesUseCase} instance.
     *
     * @return lazily initialized {@link GetDownloadedLanguagesUseCase}
     */
    public GetDownloadedLanguagesUseCase getGetDownloadedLanguagesUseCase() {
        if (getDownloadedLanguagesUseCase == null) {
            getDownloadedLanguagesUseCase = new GetDownloadedLanguagesUseCase(
                    provideLanguageModelRepository()
            );
        }
        return getDownloadedLanguagesUseCase;
    }
}