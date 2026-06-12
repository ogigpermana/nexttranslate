package com.igoy86.nexttranslate.presentation.collection;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.domain.usecase.collection.GetWordsInCollectionUseCase;
import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * ViewModel for {@link CollectionDetailFragment}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *     <li>Expose the word list for a specific collection via {@link LiveData}.</li>
 *     <li>Handle delete word operations on a background thread.</li>
 * </ul>
 *
 * <p>Instantiated via {@link CollectionDetailViewModelFactory}.</p>
 */
public class CollectionDetailViewModel extends BaseViewModel {

    private static final String TAG = "CollectionDetailViewModel";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @NonNull private final GetWordsInCollectionUseCase getWordsInCollectionUseCase;
    @NonNull private final CollectionWordRepository collectionWordRepository;
    @NonNull private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // UI State
    // -------------------------------------------------------------------------

    /**
     * LiveData emitting the list of words in the current collection.
     * Room updates this automatically when the underlying data changes.
     * Uses MediatorLiveData to safely switch sources without breaking observers.
     */
    @NonNull
    private final MediatorLiveData<List<CollectionWordItem>> wordsLiveData = new MediatorLiveData<>();

    /**
     * One-shot Snackbar message (e.g. after a word is deleted).
     * Fragment must call {@link #clearSnackbarMessage()} after consuming.
     */
    @NonNull
    private final MutableLiveData<String> snackbarMessageLiveData = new MutableLiveData<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionDetailViewModel}.
     *
     * @param getWordsInCollectionUseCase use case for observing words in a collection
     * @param collectionWordRepository    repository for delete operations
     * @param appExecutors                executor pools for threading
     */
    public CollectionDetailViewModel(
            @NonNull GetWordsInCollectionUseCase getWordsInCollectionUseCase,
            @NonNull CollectionWordRepository collectionWordRepository,
            @NonNull AppExecutors appExecutors
    ) {
        this.getWordsInCollectionUseCase = getWordsInCollectionUseCase;
        this.collectionWordRepository = collectionWordRepository;
        this.appExecutors = appExecutors;
        FileLogger.d(TAG, "CollectionDetailViewModel initialized.");
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Loads the word list for the given collection ID.
     *
     * <p>Should be called once after the Fragment is created, passing the
     * collection ID received via arguments.</p>
     *
     * @param collectionId the database ID of the collection to observe
     */
    public void loadWords(long collectionId) {
        final LiveData<List<CollectionWordItem>> source =
                getWordsInCollectionUseCase.execute(collectionId);
        wordsLiveData.addSource(source, wordsLiveData::setValue);
        FileLogger.d(TAG, "Loading words for collectionId=" + collectionId);
    }

    /**
     * Deletes a word from the collection by its database ID.
     *
     * <p>Executed on a background disk I/O thread. Shows a Snackbar on completion.</p>
     *
     * @param item the word item to delete
     */
    public void deleteWord(@NonNull CollectionWordItem item) {
        appExecutors.diskIO().execute(() -> {
            try {
                collectionWordRepository.deleteWord(item.getId());
                FileLogger.d(TAG, "Deleted word: id=" + item.getId() + " word=" + item.getWord());
                appExecutors.mainThread().execute(() ->
                        snackbarMessageLiveData.setValue("\"" + item.getWord() + "\" removed.")
                );
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to delete word: id=" + item.getId(), ex);
                appExecutors.mainThread().execute(() ->
                        snackbarMessageLiveData.setValue("Failed to delete \"" + item.getWord() + "\".")
                );
            }
        });
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

    /** @return LiveData emitting the list of words in the current collection */
    @NonNull
    public LiveData<List<CollectionWordItem>> getWordsLiveData() {
        return wordsLiveData;
    }

    /** @return LiveData emitting one-shot Snackbar messages */
    @NonNull
    public LiveData<String> getSnackbarMessageLiveData() {
        return snackbarMessageLiveData;
    }
}
