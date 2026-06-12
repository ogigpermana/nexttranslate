package com.igoy86.nexttranslate.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.igoy86.nexttranslate.data.local.entity.CollectionWordEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for the {@code collection_words} table.
 *
 * <p>Provides the SQL operations required to manage words saved inside
 * user collections. All methods in this interface are implemented
 * automatically by Room at compile time.</p>
 *
 * <p>This interface belongs to the data layer. The domain layer never
 * interacts with this DAO directly — it communicates via the
 * {@link com.igoy86.nexttranslate.domain.repository.CollectionWordRepository}
 * interface implemented by
 * {@link com.igoy86.nexttranslate.data.repository.CollectionWordRepositoryImpl}.</p>
 *
 * <p>Threading rules:</p>
 * <ul>
 *     <li>Write operations ({@link #insert}, {@link #deleteById},
 *         {@link #deleteAllByCollection}) must be called from a background thread.</li>
 *     <li>Read operations return {@link LiveData}, which Room updates
 *         automatically on the main thread.</li>
 * </ul>
 */
@Dao
public interface CollectionWordDao {

    /**
     * Inserts a new {@link CollectionWordEntity} into the {@code collection_words} table.
     *
     * <p>If a record with the same primary key already exists, it will be
     * ignored ({@link OnConflictStrategy#IGNORE}) to prevent duplicates.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param entity the word entity to insert; must not be null
     * @return the auto-generated row ID of the inserted record,
     *         or {@code -1} if the insert was ignored due to conflict
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CollectionWordEntity entity);

    /**
     * Returns a {@link LiveData} list of all words belonging to the
     * specified collection, ordered from newest to oldest by saved timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying table changes. Observed on the main thread.</p>
     *
     * @param collectionId the ID of the parent collection to query
     * @return a {@link LiveData} emitting the list of {@link CollectionWordEntity} objects
     */
    @Query("SELECT * FROM collection_words WHERE collection_id = :collectionId ORDER BY added_at DESC")
    LiveData<List<CollectionWordEntity>> getWordsByCollection(long collectionId);

    /**
     * Returns a {@link LiveData} integer representing the total number of
     * words saved inside the specified collection.
     *
     * <p>Room automatically updates this value whenever words are added
     * or removed from the collection. Useful for displaying a word count
     * badge on collection cards.</p>
     *
     * @param collectionId the ID of the parent collection to count
     * @return a {@link LiveData} emitting the current word count
     */
    @Query("SELECT COUNT(*) FROM collection_words WHERE collection_id = :collectionId")
    LiveData<Integer> getWordCount(long collectionId);

    /**
     * Deletes a single word record identified by its primary key.
     *
     * <p>If no record with the given ID exists, this operation is a no-op.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param wordId the primary key of the word record to delete
     */
    @Query("DELETE FROM collection_words WHERE id = :wordId")
    void deleteById(long wordId);

    /**
     * Deletes all word records belonging to the specified collection.
     *
     * <p>Typically called before deleting the parent collection itself,
     * though the {@link androidx.room.ForeignKey#CASCADE} constraint on
     * the {@code collection_id} column handles this automatically when
     * the parent collection row is deleted.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param collectionId the ID of the collection whose words should be deleted
     */
    @Query("DELETE FROM collection_words WHERE collection_id = :collectionId")
    void deleteAllByCollection(long collectionId);
}
