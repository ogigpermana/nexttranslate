package com.igoy86.nexttranslate.domain.usecase.collection;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.CollectionRepository;

/**
 * Use case for renaming an existing user collection.
 *
 * <p>This use case belongs to the domain layer. It encapsulates the business
 * rule of renaming a collection and delegates to {@link CollectionRepository}.</p>
 *
 * <p>Instantiated and provided by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class RenameCollectionUseCase {

    @NonNull
    private final CollectionRepository collectionRepository;

    /**
     * Constructs a new {@link RenameCollectionUseCase}.
     *
     * @param collectionRepository the repository for collection operations; must not be null
     */
    public RenameCollectionUseCase(@NonNull CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    /**
     * Executes the collection rename.
     *
     * <p>Dispatched on a background disk I/O thread. Fire-and-forget.</p>
     *
     * @param collectionId the unique database ID of the collection to rename
     * @param newName      the new display name; must not be null or empty
     */
    public void execute(long collectionId, @NonNull String newName) {
        collectionRepository.renameCollection(collectionId, newName);
    }
}
