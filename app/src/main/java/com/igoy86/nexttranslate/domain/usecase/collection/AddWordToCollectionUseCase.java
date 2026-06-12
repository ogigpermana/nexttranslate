package com.igoy86.nexttranslate.domain.usecase.collection;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;

/**
 * Use case responsible for saving a word into a user-defined collection.
 *
 * <p>Triggered when the user taps "Save to Collection" from either the
 * Dictionary result card in {@link com.igoy86.nexttranslate.presentation.collection.CollectionFragment}
 * or the translation result panel in
 * {@link com.igoy86.nexttranslate.presentation.translate.TranslateFragment}.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link CollectionWordRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     AddWordToCollectionUseCase useCase = container.getAddWordToCollectionUseCase();
 *     useCase.execute(collectionId, "serendipity", "Finding good things by chance",
 *         () -> showSnackbar("Word saved!"));
 * </pre>
 */
public class AddWordToCollectionUseCase {

    /** Repository used to persist the word entry. */
    @NonNull
    private final CollectionWordRepository repository;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link AddWordToCollectionUseCase} with the given repository.
     *
     * @param repository the repository used to insert word entries; must not be null
     */
    public AddWordToCollectionUseCase(@NonNull CollectionWordRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    /**
     * Executes the use case by inserting the given word into the specified collection.
     *
     * <p>The actual database write is performed on a background disk I/O thread.
     * The {@code onDone} callback is invoked on the main thread after the
     * insert completes, allowing the caller to update UI state safely.</p>
     *
     * @param collectionId the ID of the target collection; must reference an existing collection
     * @param word         the word or phrase to save; must not be null or empty
     * @param definition   a short definition or context for the word; must not be null
     * @param onDone       a {@link Runnable} invoked on the main thread after insertion;
     *                     must not be null
     */
    public void execute(
            long collectionId,
            @NonNull String word,
            @NonNull String definition,
            @NonNull Runnable onDone
    ) {
        final CollectionWordItem item = new CollectionWordItem(
                0L,
                collectionId,
                word,
                definition,
                System.currentTimeMillis()
        );
        repository.addWord(item, onDone);
    }
}
