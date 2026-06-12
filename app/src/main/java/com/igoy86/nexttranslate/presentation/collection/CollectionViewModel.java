package com.igoy86.nexttranslate.presentation.collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.model.DictionaryEntry;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;
import com.igoy86.nexttranslate.domain.repository.DictionaryRepository;
import com.igoy86.nexttranslate.domain.usecase.collection.CreateCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.DeleteCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.GetCollectionsUseCase;
import com.igoy86.nexttranslate.domain.usecase.dictionary.ExplainWordUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.RenameCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.dictionary.LookupWordUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * ViewModel for the {@link CollectionFragment} screen.
 *
 * <p>Manages two concerns:</p>
 * <ol>
 *     <li><strong>Collections</strong> — CRUD operations for user-created
 *         collections backed by Room via {@link GetCollectionsUseCase},
 *         {@link CreateCollectionUseCase}, {@link DeleteCollectionUseCase},
 *         and {@link RenameCollectionUseCase}.</li>
 *     <li><strong>Dictionary lookup</strong> — single-word lookups via the
 *         Free Dictionary API using {@link LookupWordUseCase}.</li>
 * </ol>
 *
 * <p>Survives configuration changes and exposes state via {@link LiveData}.</p>
 *
 * <p>Instantiated via {@link CollectionViewModelFactory}.</p>
 */
public class CollectionViewModel extends BaseViewModel {

    /** Tag used for logging events originating from this ViewModel. */
    private static final String TAG = "CollectionViewModel";

    // -------------------------------------------------------------------------
    // Use Cases
    // -------------------------------------------------------------------------

    @NonNull private final GetCollectionsUseCase getCollectionsUseCase;
    @NonNull private final CreateCollectionUseCase createCollectionUseCase;
    @NonNull private final DeleteCollectionUseCase deleteCollectionUseCase;
    @NonNull private final RenameCollectionUseCase renameCollectionUseCase;
    @NonNull private final LookupWordUseCase lookupWordUseCase;
    @NonNull private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // UI State LiveData
    // -------------------------------------------------------------------------

    /**
     * The full list of user collections ordered newest to oldest.
     * Room automatically updates this whenever the collections table changes.
     */
    @NonNull
    private final LiveData<List<CollectionItem>> collectionsLiveData;

    /**
     * The dictionary lookup result — a list of {@link DictionaryEntry} objects
     * (one per part-of-speech). Null when no lookup has been performed yet.
     */
    @NonNull
    private final MutableLiveData<List<DictionaryEntry>> dictionaryResultLiveData
            = new MutableLiveData<>();

    /**
     * Whether a dictionary lookup network call is currently in progress.
     * {@code true} while loading, {@code false} when idle.
     */
    @NonNull
    private final MutableLiveData<Boolean> dictionaryLoadingLiveData
            = new MutableLiveData<>(false);

    /**
     * Dictionary lookup error message. Null when no error is pending.
     * Fragment must call {@link #clearDictionaryError()} after consuming.
     */
    @NonNull
    private final MutableLiveData<String> dictionaryErrorLiveData
            = new MutableLiveData<>();

    /**
     * One-shot Snackbar message for collection CRUD operations (create/rename/delete).
     * Fragment must call {@link #clearSnackbarMessage()} after consuming.
     */
    @NonNull
    private final MutableLiveData<String> snackbarMessageLiveData
            = new MutableLiveData<>();
			
	/**
     * The AI explanation result for the last looked-up word.
     * Null when no explain has been requested yet.
     */
    @NonNull
    private final MutableLiveData<String> explainResultLiveData = new MutableLiveData<>();

    /**
     * Whether an AI explain request is currently in progress.
     */
    @NonNull
    private final MutableLiveData<Boolean> explainLoadingLiveData = new MutableLiveData<>(false);

    /**
     * AI explain error message. Null when no error is pending.
     * Fragment must call {@link #clearExplainError()} after consuming.
     */
    @NonNull
    private final MutableLiveData<String> explainErrorLiveData = new MutableLiveData<>();

    // Field dependency:
    @NonNull private final ExplainWordUseCase explainWordUseCase;

    /** Observer for the explain LiveData, stored to allow cleanup on ViewModel clear. */
    @Nullable
    private Observer<com.igoy86.nexttranslate.util.Resource<String>> explainObserver;
	
	// Getters:
    @NonNull
    public LiveData<String> getExplainResultLiveData() { return explainResultLiveData; }

    @NonNull
    public LiveData<Boolean> getExplainLoadingLiveData() { return explainLoadingLiveData; }

    @NonNull
    public LiveData<String> getExplainErrorLiveData() { return explainErrorLiveData; }
    
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionViewModel} with all required dependencies.
     *
     * @param getCollectionsUseCase    use case for observing all collections
     * @param createCollectionUseCase  use case for creating a new collection
     * @param deleteCollectionUseCase  use case for deleting a collection
     * @param renameCollectionUseCase  use case for renaming a collection
     * @param lookupWordUseCase        use case for dictionary word lookup
     * @param appExecutors             executor pools for threading
     */
    public CollectionViewModel(
        @NonNull GetCollectionsUseCase getCollectionsUseCase,
        @NonNull CreateCollectionUseCase createCollectionUseCase,
        @NonNull DeleteCollectionUseCase deleteCollectionUseCase,
        @NonNull RenameCollectionUseCase renameCollectionUseCase,
        @NonNull LookupWordUseCase lookupWordUseCase,
        @NonNull ExplainWordUseCase explainWordUseCase,
        @NonNull AppExecutors appExecutors
    ) {
        this.getCollectionsUseCase = getCollectionsUseCase;
        this.createCollectionUseCase = createCollectionUseCase;
        this.deleteCollectionUseCase = deleteCollectionUseCase;
        this.renameCollectionUseCase = renameCollectionUseCase;
        this.lookupWordUseCase = lookupWordUseCase;
		this.explainWordUseCase = explainWordUseCase;
        this.appExecutors = appExecutors;
        this.collectionsLiveData = getCollectionsUseCase.execute();
        FileLogger.d(TAG, "CollectionViewModel initialized.");
    }

    // -------------------------------------------------------------------------
    // Collection actions
    // -------------------------------------------------------------------------

    /**
     * Creates a new user collection with the given name and accent colour.
     *
     * @param name     the display name for the collection; must not be null or empty
     * @param colorHex the accent colour hex string (e.g. "#00897B"); must not be null
     */
    public void createCollection(@NonNull String name, @NonNull String colorHex) {
        createCollectionUseCase.execute(name, colorHex, generatedId -> {
            FileLogger.d(TAG, "Collection created: id=" + generatedId);
            appExecutors.mainThread().execute(() ->
                    snackbarMessageLiveData.setValue("Collection \"" + name + "\" created.")
            );
        });
    }

    /**
     * Renames an existing collection.
     *
     * @param id      the database ID of the collection to rename
     * @param newName the new display name; must not be null or empty
     */
    public void renameCollection(long id, @NonNull String newName) {
        renameCollectionUseCase.execute(id, newName);
        snackbarMessageLiveData.setValue("Collection renamed.");
        FileLogger.d(TAG, "Rename collection: id=" + id + " newName=" + newName);
    }

    /**
     * Deletes a collection by its database ID.
     *
     * @param id the database ID of the collection to delete
     */
    public void deleteCollection(long id) {
        deleteCollectionUseCase.execute(id);
        snackbarMessageLiveData.setValue("Collection deleted.");
        FileLogger.d(TAG, "Delete collection: id=" + id);
    }

    // -------------------------------------------------------------------------
    // Dictionary actions
    // -------------------------------------------------------------------------

    /**
     * Looks up the given word via the Free Dictionary API.
     *
     * <p>Sets {@link #dictionaryLoadingLiveData} to {@code true} while the
     * request is in progress. On completion, populates either
     * {@link #dictionaryResultLiveData} or {@link #dictionaryErrorLiveData}.</p>
     *
     * @param word the word to look up; must not be null or empty
     */
    public void lookupWord(@NonNull String word) {
        if (word.trim().isEmpty()) return;

        // Clear previous result/error and show loading
        dictionaryResultLiveData.setValue(null);
        dictionaryErrorLiveData.setValue(null);
        dictionaryLoadingLiveData.setValue(true);

        FileLogger.d(TAG, "Dictionary lookup: " + word);

        lookupWordUseCase.execute("all", word, new DictionaryRepository.DictionaryCallback() {
            @Override
            public void onSuccess(@NonNull List<DictionaryEntry> entries) {
                appExecutors.mainThread().execute(() -> {
                    dictionaryLoadingLiveData.setValue(false);
                    dictionaryResultLiveData.setValue(entries);
                    FileLogger.d(TAG, "Dictionary result: " + entries.size() + " entries");
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                appExecutors.mainThread().execute(() -> {
                    dictionaryLoadingLiveData.setValue(false);
                    dictionaryErrorLiveData.setValue(errorMessage);
                    FileLogger.d(TAG, "Dictionary error: " + errorMessage);
                });
            }
        });
    }
	
	/**
     * Requests an AI explanation for the given word.
     *
     * <p>Sets {@link #explainLoadingLiveData} to {@code true} while in progress.
     * On completion, populates either {@link #explainResultLiveData} or
     * {@link #explainErrorLiveData}.</p>
     *
     * @param word       the word to explain; must not be null or empty
     * @param language   the full English name of the word's language
     * @param definition an optional short definition for AI context; may be null
     */
    public void explainWord(@NonNull String word, @NonNull String language, @Nullable String definition) {
        if (word.trim().isEmpty()) return;

        explainResultLiveData.setValue(null);
        explainErrorLiveData.setValue(null);
        explainLoadingLiveData.setValue(true);

        FileLogger.d(TAG, "Explain request: " + word);

        // Remove previous observer to prevent leaks
        if (explainObserver != null) {
            explainWordUseCase.execute(word, language, definition).removeObserver(explainObserver);
        }

        explainObserver = resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case SUCCESS:
                    explainLoadingLiveData.setValue(false);
                    explainResultLiveData.setValue(resource.getData());
                    FileLogger.d(TAG, "Explain success: " + word);
                    break;
                case ERROR:
                    explainLoadingLiveData.setValue(false);
                    explainErrorLiveData.setValue(resource.getMessage());
                    FileLogger.d(TAG, "Explain error: " + resource.getMessage());
                    break;
                case LOADING:
                    break;
            }
        };
        explainWordUseCase.execute(word, language, definition).observeForever(explainObserver);
    }

    /** Clears the pending explain error after it has been consumed by the Fragment. */
    public void clearExplainError() {
        explainErrorLiveData.setValue(null);
    }

    /**
     * Clears the pending dictionary error after it has been consumed by the Fragment.
     */
    public void clearDictionaryError() {
        dictionaryErrorLiveData.setValue(null);
    }

    /**
     * Clears the pending Snackbar message after it has been consumed by the Fragment.
     */
    public void clearSnackbarMessage() {
        snackbarMessageLiveData.setValue(null);
    }

    // -------------------------------------------------------------------------
    // LiveData accessors
    // -------------------------------------------------------------------------

    /** @return LiveData emitting the full list of user collections */
    @NonNull
    public LiveData<List<CollectionItem>> getCollectionsLiveData() {
        return collectionsLiveData;
    }

    /** @return LiveData emitting dictionary lookup results */
    @NonNull
    public LiveData<List<DictionaryEntry>> getDictionaryResultLiveData() {
        return dictionaryResultLiveData;
    }

    /** @return LiveData emitting {@code true} while a lookup is in progress */
    @NonNull
    public LiveData<Boolean> getDictionaryLoadingLiveData() {
        return dictionaryLoadingLiveData;
    }

    /** @return LiveData emitting the dictionary error message, or null if none */
    @NonNull
    public LiveData<String> getDictionaryErrorLiveData() {
        return dictionaryErrorLiveData;
    }

    /** @return LiveData emitting one-shot Snackbar messages for collection CRUD */
    @NonNull
    public LiveData<String> getSnackbarMessageLiveData() {
        return snackbarMessageLiveData;
    }
	
	/** Clears dictionary result LiveData (called when user click X icon). */
    public void clearDictionaryResult() {
        // if use MutableLiveData directly:
        ((MutableLiveData<List<DictionaryEntry>>) dictionaryResultLiveData).setValue(null);
    }

    /** Clears explain result LiveData (called when user tap X icon). */
    public void clearExplainResult() {
        explainResultLiveData.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (explainObserver != null) {
            // We cannot remove from a specific LiveData without the reference,
            // but the observer is lifecycle-unaware. Since the ViewModel is cleared,
            // the LiveData will be GC'd and the observer will not fire again.
            explainObserver = null;
        }
    }
}
