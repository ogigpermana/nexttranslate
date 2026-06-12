package com.igoy86.nexttranslate.domain.usecase.collection;

import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;

import java.util.List;

/**
 * Use case responsible for retrieving all words saved inside a specific collection.
 *
 * <p>Observed by the presentation layer to display the word list when the
 * user opens a collection detail screen.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link CollectionWordRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     GetWordsInCollectionUseCase useCase = container.getGetWordsInCollectionUseCase();
 *     useCase.execute(collectionId).observe(viewLifecycleOwner, words -> {
 *         adapter.submitList(words);
 *     });
 * </pre>
 */
public class GetWordsInCollectionUseCase {

    /** Repository used to observe the word list for a collection. */
    private final CollectionWordRepository repository;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link GetWordsInCollectionUseCase} with the given repository.
     *
     * @param repository the repository used to observe word entries; must not be null
     */
    public GetWordsInCollectionUseCase(CollectionWordRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link LiveData} list of all words belonging to the specified collection,
     * ordered from newest to oldest by saved timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever the
     * underlying data changes. The caller should observe this on the
     * {@code viewLifecycleOwner} to avoid memory leaks.</p>
     *
     * @param collectionId the ID of the collection to observe
     * @return a {@link LiveData} emitting the list of {@link CollectionWordItem} objects
     */
    public LiveData<List<CollectionWordItem>> execute(long collectionId) {
        return repository.getWordsByCollection(collectionId);
    }
}
