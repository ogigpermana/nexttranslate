package com.igoy86.nexttranslate.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.igoy86.nexttranslate.data.local.entity.CollectionEntity;
import com.igoy86.nexttranslate.data.local.dto.CollectionWithWordCount;

import java.util.List;

/**
 * Data Access Object (DAO) for the {@code collections} table.
 *
 * <p>Provides the SQL operations required to manage user collection records
 * in the Room database. All methods in this interface are implemented
 * automatically by Room at compile time.</p>
 *
 * <p>This interface belongs to the data layer. The domain layer never
 * interacts with this DAO directly — it communicates via the
 * {@link com.igoy86.nexttranslate.domain.repository.CollectionRepository}
 * interface implemented by
 * {@link com.igoy86.nexttranslate.data.repository.CollectionRepositoryImpl}.</p>
 *
 * <p>Threading rules:</p>
 * <ul>
 *     <li>Write operations must be called from a background thread.</li>
 *     <li>Read operations return {@link LiveData}, which Room updates
 *         automatically on the main thread.</li>
 * </ul>
 */
@Dao
public interface CollectionDao {

    /**
     * Inserts a new {@link CollectionEntity} into the {@code collections} table
     * and returns the auto-generated row ID.
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param entity the collection entity to insert; must not be null
     * @return the auto-generated primary key of the newly inserted row
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CollectionEntity entity);

    /**
     * Returns a {@link LiveData} list of all collections joined with their
     * word count from the {@code collection_words} table.
     *
     * <p>Uses a LEFT JOIN so collections with zero words are still included,
     * with {@code wordCount} defaulting to {@code 0} via {@code COALESCE}.
     * Room automatically invalidates and re-emits this list whenever either
     * the {@code collections} or {@code collection_words} table changes.</p>
     *
     * @return a {@link LiveData} emitting the list of {@link CollectionWithWordCount}
     */
    @Query("SELECT c.*, COALESCE(COUNT(w.id), 0) AS wordCount " +
           "FROM collections c " +
           "LEFT JOIN collection_words w ON w.collection_id = c.id " +
           "GROUP BY c.id " +
           "ORDER BY c.created_at DESC")
    LiveData<List<CollectionWithWordCount>> getAllCollectionsWithWordCount();

    /**
     * Returns a {@link LiveData} list of all collection records ordered
     * from newest to oldest by creation timestamp.
     *
     * @return a {@link LiveData} emitting the full list of {@link CollectionEntity} objects
     */
    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    LiveData<List<CollectionEntity>> getAllCollections();

    /**
     * Updates the name of an existing collection identified by its primary key.
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param id   the primary key of the collection to rename
     * @param name the new display name; must not be null
     */
    @Query("UPDATE collections SET name = :name WHERE id = :id")
    void rename(long id, String name);

    /**
     * Updates the accent color of an existing collection.
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param id       the primary key of the collection to update
     * @param colorHex the new accent color hex string (e.g. "#FF5252")
     */
    @Query("UPDATE collections SET color_hex = :colorHex WHERE id = :id")
    void updateColor(long id, String colorHex);

    /**
     * Deletes a single collection record identified by its primary key.
     *
     * <p>If no record with the given ID exists, this operation is a no-op.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param id the primary key of the collection to delete
     */
    @Query("DELETE FROM collections WHERE id = :id")
    void deleteById(long id);

    /**
     * Deletes all records from the {@code collections} table.
     *
     * <p>This operation is irreversible. Must be called from a background thread.</p>
     */
    @Query("DELETE FROM collections")
    void clearAll();

    /**
     * Returns the total number of records in the {@code collections} table.
     *
     * @return the total count of collection records
     */
    @Query("SELECT COUNT(*) FROM collections")
    int getCount();
}
