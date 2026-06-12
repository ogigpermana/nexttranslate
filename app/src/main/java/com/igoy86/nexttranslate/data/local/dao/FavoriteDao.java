package com.igoy86.nexttranslate.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.igoy86.nexttranslate.data.local.entity.FavoriteEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for the {@code favorites} table.
 *
 * <p>Provides the SQL operations required to manage favorite translation
 * records in the Room database. All methods in this interface are
 * implemented automatically by Room at compile time.</p>
 *
 * <p>This interface belongs to the data layer. The domain layer never
 * interacts with this DAO directly — it communicates via the
 * {@link com.igoy86.nexttranslate.domain.repository.FavoriteRepository}
 * interface implemented by
 * {@link com.igoy86.nexttranslate.data.repository.FavoriteRepositoryImpl}.</p>
 *
 * <p>Threading rules:</p>
 * <ul>
 *     <li>Write operations ({@link #insert}, {@link #deleteById})
 *         must be called from a background thread.</li>
 *     <li>Read operations return {@link LiveData}, which Room updates
 *         automatically on the main thread.</li>
 * </ul>
 */
@Dao
public interface FavoriteDao {

    /**
     * Inserts a new {@link FavoriteEntity} into the {@code favorites} table.
     *
     * <p>If a record with the same primary key already exists, it will be
     * replaced ({@link OnConflictStrategy#REPLACE}).</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param entity the favorite entity to insert; must not be null
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity entity);

    /**
     * Returns a {@link LiveData} list of all favorite records in the
     * {@code favorites} table, ordered from newest to oldest by saved timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying table changes. Observed on the main thread.</p>
     *
     * @return a {@link LiveData} emitting the full list of {@link FavoriteEntity} objects
     */
    @Query("SELECT * FROM favorites ORDER BY saved_at DESC")
    LiveData<List<FavoriteEntity>> getAllFavorites();

    /**
     * Deletes a single favorite record identified by its primary key.
     *
     * <p>If no record with the given ID exists, this operation is a no-op.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param id the primary key of the favorite record to delete
     */
    @Query("DELETE FROM favorites WHERE id = :id")
    void deleteById(long id);

    /**
     * Checks whether a translation with the given source text and language pair
     * already exists in the {@code favorites} table.
     *
     * <p>Useful for toggling the favorite/bookmark icon state on the
     * translate screen without querying the full list.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param sourceText         the original text to check
     * @param sourceLanguageCode the BCP-47 source language code
     * @param targetLanguageCode the BCP-47 target language code
     * @return the count of matching records; {@code 0} means not favorited,
     *         {@code 1} means already favorited
     */
    @Query("SELECT COUNT(*) FROM favorites " +
           "WHERE source_text = :sourceText " +
           "AND source_language = :sourceLanguageCode " +
           "AND target_language = :targetLanguageCode")
    int isFavorited(String sourceText, String sourceLanguageCode, String targetLanguageCode);

    /**
     * Returns the total number of records in the {@code favorites} table.
     *
     * <p>Useful for checking whether the favorites list is empty before
     * showing an empty state UI.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @return the total count of favorite records
     */
    @Query("SELECT COUNT(*) FROM favorites")
    int getFavoritesCount();
	
	/**
     * Deletes all entries from the favorites table.
     * Called by {@link com.igoy86.nexttranslate.data.repository.FavoriteRepositoryImpl#clearAllFavorites()}.
     */
    @Query("DELETE FROM favorites")
    void clearAll();
}