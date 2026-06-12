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
import com.igoy86.nexttranslate.domain.usecase.history.UpdateHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.RestoreHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.AddFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.GetAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.RestoreFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.ClearAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.DeleteFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.TranslateTextUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.DetectLanguageUseCase;
import com.igoy86.nexttranslate.data.remote.api.DictionaryApiService;
import com.igoy86.nexttranslate.data.remote.datasource.DictionaryRemoteDataSource;
import com.igoy86.nexttranslate.data.repository.DictionaryRepositoryImpl;
import com.igoy86.nexttranslate.domain.repository.DictionaryRepository;
import com.igoy86.nexttranslate.domain.usecase.dictionary.LookupWordUseCase;
import com.igoy86.nexttranslate.data.remote.api.TranslateApiService;
import com.igoy86.nexttranslate.data.remote.datasource.RemoteTranslateDataSource;
import com.igoy86.nexttranslate.data.repository.RemoteTranslateRepositoryImpl;
import com.igoy86.nexttranslate.domain.repository.RemoteTranslateRepository;
import com.igoy86.nexttranslate.domain.usecase.translate.RemoteTranslateTextUseCase;
import com.igoy86.nexttranslate.data.local.dao.CollectionDao;
import com.igoy86.nexttranslate.data.repository.CollectionRepositoryImpl;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;
import com.igoy86.nexttranslate.domain.usecase.collection.GetCollectionsUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.CreateCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.DeleteCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.RenameCollectionUseCase;
import com.igoy86.nexttranslate.data.local.dao.CollectionWordDao;
import com.igoy86.nexttranslate.data.repository.CollectionWordRepositoryImpl;
import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;
import com.igoy86.nexttranslate.domain.usecase.collection.AddWordToCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.GetWordsInCollectionUseCase;

import com.igoy86.nexttranslate.data.remote.datasource.ExplainDataSource;
import com.igoy86.nexttranslate.data.repository.ExplainRepositoryImpl;
import com.igoy86.nexttranslate.domain.repository.ExplainRepository;
import com.igoy86.nexttranslate.domain.usecase.dictionary.ExplainWordUseCase;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.igoy86.nexttranslate.domain.usecase.language.DownloadLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.DeleteLanguageModelUseCase;
import com.igoy86.nexttranslate.domain.usecase.language.GetDownloadedLanguagesUseCase;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.BuildConfig;

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
	
	private static final String BASE_URL = BuildConfig.BACKEND_BASE_URL;
    private static final String APP_TOKEN = BuildConfig.APP_TOKEN;

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
	
	/** Data Access Object for user collection entries. */
    private CollectionDao collectionDao;

    /** Repository handling user collections persistence. */
    private CollectionRepository collectionRepository;

    /** Use case for retrieving all user collections. */
    private GetCollectionsUseCase getCollectionsUseCase;

    /** Use case for creating a new user collection. */
    private CreateCollectionUseCase createCollectionUseCase;

    /** Use case for deleting a user collection. */
    private DeleteCollectionUseCase deleteCollectionUseCase;

    /** Use case for renaming a user collection. */
    private RenameCollectionUseCase renameCollectionUseCase;

    /** DAO for collection_words table operations. */
    private CollectionWordDao collectionWordDao;

    /** Repository handling word persistence within user collections. */
    private CollectionWordRepository collectionWordRepository;

    /** Use case for saving a word into a collection. */
    private AddWordToCollectionUseCase addWordToCollectionUseCase;

    /** Use case for retrieving all words within a collection. */
    private GetWordsInCollectionUseCase getWordsInCollectionUseCase;

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
	
	/** Use case for updating an existing translation history entry in-place. */
    private UpdateHistoryUseCase updateHistoryUseCase;
	
	 /** Use case for restoring a deleted history entry (Undo swipe-delete). */
    private RestoreHistoryUseCase restoreHistoryUseCase;

    // -------------------------------------------------------------------------
    // Use Cases — Favorite
    // -------------------------------------------------------------------------

    /** Use case for adding a translation to favorites. */
    private AddFavoriteUseCase addFavoriteUseCase;

    /** Use case for retrieving all favorite translations. */
    private GetAllFavoritesUseCase getAllFavoritesUseCase;

    /** Use case for deleting a favorite translation. */
    private DeleteFavoriteUseCase deleteFavoriteUseCase;
	
	/** Use case for restoring a deleted favorite entry (Undo swipe-delete). */
    private RestoreFavoriteUseCase restoreFavoriteUseCase;
	
	/** Use case for clear all favorites translation */
	private ClearAllFavoritesUseCase clearAllFavoritesUseCase;

    // -------------------------------------------------------------------------
    // Use Cases — Translate
    // -------------------------------------------------------------------------

    /** Use case for performing text translation via ML Kit. */
    private TranslateTextUseCase translateTextUseCase;

    /** Use case for detecting the language of a given text. */
    private DetectLanguageUseCase detectLanguageUseCase;
	
	/** Use case for remote the language of a given text. */
	private RemoteTranslateRepository remoteTranslateRepository;
    private RemoteTranslateTextUseCase remoteTranslateTextUseCase;
	
	/** Repository handling Free Dictionary API lookups. */
    private DictionaryRepository dictionaryRepository;

    /** Use case for looking up a word in the dictionary. */
    private LookupWordUseCase lookupWordUseCase;
	
	/** Repository for AI word explanation. */
    private ExplainRepository explainRepository;

    /** Use case for AI word explanation. */
    private ExplainWordUseCase explainWordUseCase;

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
		collectionDao = appDatabase.collectionDao();
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
     * Lazily initializes and returns the {@link CollectionRepository}.
     *
     * @return the singleton {@link CollectionRepository} instance
     */
    private CollectionRepository provideCollectionRepository() {
        if (collectionRepository == null) {
            collectionRepository = new CollectionRepositoryImpl(collectionDao, appExecutors);
            FileLogger.d(TAG, "CollectionRepository created.");
        }
        return collectionRepository;
    }
	
    private ExplainRepository provideExplainRepository() {
        if (explainRepository == null) {
            final TranslateApiService apiService = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(TranslateApiService.class);
            final ExplainDataSource dataSource = new ExplainDataSource(apiService, APP_TOKEN);
            explainRepository = new ExplainRepositoryImpl(dataSource, appExecutors);
            FileLogger.d(TAG, "ExplainRepository created.");
        }
        return explainRepository;
    }

    /**
     * Returns the {@link ExplainWordUseCase} instance.
     *
     * <p>Used by {@code CollectionViewModel} to request AI word explanations.</p>
     *
     * @return lazily initialized {@link ExplainWordUseCase}
     */
    public ExplainWordUseCase getExplainWordUseCase() {
        if (explainWordUseCase == null) {
            explainWordUseCase = new ExplainWordUseCase(provideExplainRepository());
        }
        return explainWordUseCase;
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
     * Lazily initializes and returns the {@link RemoteTranslateRepository}.
     *
     * <p>Creates a dedicated Retrofit instance pointing to the backend base URL
     * configured in {@code BuildConfig.BACKEND_BASE_URL}. Requests are
     * authenticated using a Bearer token from {@code BuildConfig.APP_TOKEN}.</p>
     *
     * @return the singleton {@link RemoteTranslateRepository} instance
     */
	private RemoteTranslateRepository provideRemoteTranslateRepository() {
        if (remoteTranslateRepository == null) {
            final TranslateApiService apiService = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(TranslateApiService.class);
            final RemoteTranslateDataSource dataSource =
                    new RemoteTranslateDataSource(apiService, APP_TOKEN);
            remoteTranslateRepository = new RemoteTranslateRepositoryImpl(
                    dataSource, appExecutors
            );
            FileLogger.d(TAG, "RemoteTranslateRepository created.");
        }
        return remoteTranslateRepository;
    }
	
    /**
     * Lazily initializes and returns the {@link DictionaryRepository}.
     *
     * <p>Creates a dedicated Retrofit instance pointing to the Free Dictionary
     * API base URL. No auth token is needed — the API is public and free.</p>
     *
     * @return the singleton {@link DictionaryRepository} instance
     */
    private DictionaryRepository provideDictionaryRepository() {
        if (dictionaryRepository == null) {
            final DictionaryApiService apiService = new Retrofit.Builder()
                    .baseUrl("https://freedictionaryapi.com/api/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(DictionaryApiService.class);
            final DictionaryRemoteDataSource dataSource =
                    new DictionaryRemoteDataSource(apiService);
            dictionaryRepository = new DictionaryRepositoryImpl(dataSource, appExecutors);
            FileLogger.d(TAG, "DictionaryRepository created.");
        }
        return dictionaryRepository;
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
     * Returns the {@link UpdateHistoryUseCase} instance.
     *
     * <p>Used by {@code TranslateViewModel} to update an existing history entry
     * in-place during the same translate session, instead of inserting a new row.</p>
     *
     * @return lazily initialized {@link UpdateHistoryUseCase}
     */
    public UpdateHistoryUseCase getUpdateHistoryUseCase() {
        if (updateHistoryUseCase == null) {
            updateHistoryUseCase = new UpdateHistoryUseCase(provideHistoryRepository());
        }
        return updateHistoryUseCase;
    }
	
	 /**
     * Returns the {@link RestoreHistoryUseCase} instance.
     *
     * <p>Used by {@code HistoryFragment} to undo a swipe-delete action.</p>
     *
     * @return lazily initialized {@link RestoreHistoryUseCase}
     */
    public RestoreHistoryUseCase getRestoreHistoryUseCase() {
        if (restoreHistoryUseCase == null) {
            restoreHistoryUseCase = new RestoreHistoryUseCase(provideHistoryRepository());
        }
        return restoreHistoryUseCase;
    }
	
	/**
     * Returns the {@link ClearAllFavoritesUseCase} instance.
     *
     * <p>Used by {@code HistoryFragment} to clear all favorites translation</p>
     *
     * @return lazily initialized {@link ClearAllFavoritesUseCase}
     */
	public ClearAllFavoritesUseCase getClearAllFavoritesUseCase() {
        if (clearAllFavoritesUseCase == null) {
            clearAllFavoritesUseCase = new ClearAllFavoritesUseCase(provideFavoriteRepository());
        }
        return clearAllFavoritesUseCase;
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
     * Returns the {@link RestoreFavoriteUseCase} instance.
     *
     * @return lazily initialized {@link RestoreFavoriteUseCase}
     */
    public RestoreFavoriteUseCase getRestoreFavoriteUseCase() {
        if (restoreFavoriteUseCase == null) {
            restoreFavoriteUseCase = new RestoreFavoriteUseCase(provideFavoriteRepository());
        }
        return restoreFavoriteUseCase;
    }
	
	/**
     * Returns the {@link GetCollectionsUseCase} instance.
     *
     * <p>Used by {@code CollectionViewModel} to observe all user collections.</p>
     *
     * @return lazily initialized {@link GetCollectionsUseCase}
     */
    public GetCollectionsUseCase getGetCollectionsUseCase() {
        if (getCollectionsUseCase == null) {
            getCollectionsUseCase = new GetCollectionsUseCase(provideCollectionRepository());
        }
        return getCollectionsUseCase;
    }

    /**
     * Returns the {@link CreateCollectionUseCase} instance.
     *
     * <p>Used by {@code CollectionViewModel} to create a new user collection.</p>
     *
     * @return lazily initialized {@link CreateCollectionUseCase}
     */
    public CreateCollectionUseCase getCreateCollectionUseCase() {
        if (createCollectionUseCase == null) {
            createCollectionUseCase = new CreateCollectionUseCase(provideCollectionRepository());
        }
        return createCollectionUseCase;
    }

    /**
     * Returns the {@link DeleteCollectionUseCase} instance.
     *
     * <p>Used by {@code CollectionViewModel} to delete a user collection.</p>
     *
     * @return lazily initialized {@link DeleteCollectionUseCase}
     */
    public DeleteCollectionUseCase getDeleteCollectionUseCase() {
        if (deleteCollectionUseCase == null) {
            deleteCollectionUseCase = new DeleteCollectionUseCase(provideCollectionRepository());
        }
        return deleteCollectionUseCase;
    }

    /**
     * Returns the {@link RenameCollectionUseCase} instance.
     *
     * <p>Used by {@code CollectionViewModel} to rename a user collection.</p>
     *
     * @return lazily initialized {@link RenameCollectionUseCase}
     */
    public RenameCollectionUseCase getRenameCollectionUseCase() {
        if (renameCollectionUseCase == null) {
            renameCollectionUseCase = new RenameCollectionUseCase(provideCollectionRepository());
        }
        return renameCollectionUseCase;
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
     * Returns the {@link RemoteTranslateTextUseCase} instance.
     *
     * <p>Used by {@code TranslateViewModel} to perform text translation
     * via the Groq-powered backend instead of ML Kit.</p>
     *
     * @return lazily initialized {@link RemoteTranslateTextUseCase}
     */
	public RemoteTranslateTextUseCase getRemoteTranslateTextUseCase() {
        if (remoteTranslateTextUseCase == null) {
            remoteTranslateTextUseCase = new RemoteTranslateTextUseCase(
                    provideRemoteTranslateRepository()
            );
        }
        return remoteTranslateTextUseCase;
    }
	
	/**
     * Returns the {@link LookupWordUseCase} instance.
     *
     * <p>Used by {@code DictionaryViewModel} to look up words
     * via the Free Dictionary API.</p>
     *
     * @return lazily initialized {@link LookupWordUseCase}
     */
    public LookupWordUseCase getLookupWordUseCase() {
        if (lookupWordUseCase == null) {
            lookupWordUseCase = new LookupWordUseCase(provideDictionaryRepository());
        }
        return lookupWordUseCase;
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
	
	// -------------------------------------------------------------------------
    // Collection Words
    // -------------------------------------------------------------------------

    private CollectionWordDao provideCollectionWordDao() {
        if (collectionWordDao == null) {
            collectionWordDao = appDatabase.collectionWordDao();
        }
        return collectionWordDao;
    }

    private CollectionWordRepository provideCollectionWordRepository() {
        if (collectionWordRepository == null) {
            collectionWordRepository = new CollectionWordRepositoryImpl(
                    provideCollectionWordDao(),
                    appExecutors
            );
        }
        return collectionWordRepository;
    }

    public AddWordToCollectionUseCase getAddWordToCollectionUseCase() {
        if (addWordToCollectionUseCase == null) {
            addWordToCollectionUseCase = new AddWordToCollectionUseCase(
                    provideCollectionWordRepository()
            );
        }
        return addWordToCollectionUseCase;
    }

    public GetWordsInCollectionUseCase getGetWordsInCollectionUseCase() {
        if (getWordsInCollectionUseCase == null) {
            getWordsInCollectionUseCase = new GetWordsInCollectionUseCase(
                    provideCollectionWordRepository()
            );
        }
        return getWordsInCollectionUseCase;
    }
	
	public CollectionWordRepository getCollectionWordRepository() {
        return provideCollectionWordRepository();
    }
}