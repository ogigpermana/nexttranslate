package com.igoy86.nexttranslate.domain.usecase.collection;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;

/**
 * Use case for creating a new user collection.
 *
 * <p>This use case belongs to the domain layer. It encapsulates the business
 * rule of creating a collection and delegates to {@link CollectionRepository}.</p>
 *
 * <p>Instantiated and provided by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class CreateCollectionUseCase {

    @NonNull
    private final CollectionRepository collectionRepository;

    /**
     * Constructs a new {@link CreateCollectionUseCase}.
     *
     * @param collectionRepository the repository for collection operations; must not be null
     */
    public CreateCollectionUseCase(@NonNull CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    /**
     * Executes the collection creation.
     *
     * <p>The result is delivered asynchronously via {@code callback} on a
     * background thread. Callers must post back to the main thread if
     * updating the UI.</p>
     *
     * @param name      the display name for the new collection; must not be null
     * @param colorHex  the accent color in hex format (e.g. "#FF5252"); must not be null
     * @param callback  delivers the generated ID on success; must not be null
     */
    public void execute(
            @NonNull String name,
            @NonNull String colorHex,
            @NonNull CollectionRepository.InsertCallback callback
    ) {
        final CollectionItem item = new CollectionItem(
                0L,
                name,
                colorHex,
                System.currentTimeMillis(),
				 0
        );
        collectionRepository.createCollection(item, callback);
    }
}
