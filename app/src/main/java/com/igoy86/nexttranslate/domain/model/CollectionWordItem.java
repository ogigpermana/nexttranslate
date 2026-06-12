package com.igoy86.nexttranslate.domain.model;

import androidx.annotation.NonNull;

/**
 * Domain model representing a single word saved inside a user collection.
 *
 * <p>This class belongs to the domain layer and is the primary data contract
 * between the presentation layer and the domain use cases. It has no dependency
 * on Room, Android, or any framework-specific class.</p>
 *
 * <p>Instances of this class are produced by
 * {@link com.igoy86.nexttranslate.data.mapper.CollectionWordMapper} from
 * {@link com.igoy86.nexttranslate.data.local.entity.CollectionWordEntity}
 * objects retrieved from Room.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     CollectionWordItem word = new CollectionWordItem(
 *         1L, 42L, "serendipity", "Finding something good without looking for it",
 *         System.currentTimeMillis()
 *     );
 * </pre>
 */
public class CollectionWordItem {

    /** The unique database row ID of this word record. */
    private final long id;

    /** The database ID of the parent collection this word belongs to. */
    private final long collectionId;

    /** The word or phrase saved by the user. */
    @NonNull
    private final String word;

    /** A short definition or context string for the word. */
    @NonNull
    private final String definition;

    /** Unix timestamp in milliseconds when this word was saved. */
    private final long addedAt;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionWordItem}.
     *
     * @param id           the unique database row ID
     * @param collectionId the ID of the parent collection
     * @param word         the word or phrase; must not be null
     * @param definition   a short definition or context; must not be null
     * @param addedAt      the Unix timestamp in milliseconds when the word was saved
     */
    public CollectionWordItem(
            long id,
            long collectionId,
            @NonNull String word,
            @NonNull String definition,
            long addedAt
    ) {
        this.id = id;
        this.collectionId = collectionId;
        this.word = word;
        this.definition = definition;
        this.addedAt = addedAt;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the unique database row ID of this word record.
     *
     * @return the row ID
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the database ID of the parent collection.
     *
     * @return the collection ID
     */
    public long getCollectionId() {
        return collectionId;
    }

    /**
     * Returns the word or phrase saved by the user.
     *
     * @return the word string; never null
     */
    @NonNull
    public String getWord() {
        return word;
    }

    /**
     * Returns the short definition or context for this word.
     *
     * @return the definition string; never null
     */
    @NonNull
    public String getDefinition() {
        return definition;
    }

    /**
     * Returns the Unix timestamp in milliseconds when this word was saved.
     *
     * @return the timestamp in milliseconds
     */
    public long getAddedAt() {
        return addedAt;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "CollectionWordItem{" +
                "id=" + id +
                ", collectionId=" + collectionId +
                ", word='" + word + '\'' +
                ", definition='" + definition + '\'' +
                ", addedAt=" + addedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollectionWordItem that = (CollectionWordItem) o;
        return id == that.id
                && collectionId == that.collectionId
                && addedAt == that.addedAt
                && word.equals(that.word)
                && definition.equals(that.definition);
    }

    @Override
    public int hashCode() {
        int result = Long.valueOf(id).hashCode();
        result = 31 * result + Long.valueOf(collectionId).hashCode();
        result = 31 * result + word.hashCode();
        result = 31 * result + definition.hashCode();
        result = 31 * result + Long.valueOf(addedAt).hashCode();
        return result;
    }
}
