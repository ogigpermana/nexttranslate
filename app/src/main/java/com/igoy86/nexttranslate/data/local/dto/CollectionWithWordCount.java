package com.igoy86.nexttranslate.data.local.dto;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.igoy86.nexttranslate.data.local.entity.CollectionEntity;

/**
 * Data Transfer Object (DTO) used by Room to map the result of the
 * {@code getAllCollectionsWithWordCount} JOIN query in
 * {@link com.igoy86.nexttranslate.data.local.dao.CollectionDao}.
 *
 * <p>Combines all columns from {@link CollectionEntity} via {@code @Embedded}
 * with an additional {@code wordCount} column derived from a LEFT JOIN
 * with the {@code collection_words} table.</p>
 *
 * <p>This class is a data-layer concern only. It is converted to a
 * {@link com.igoy86.nexttranslate.domain.model.CollectionItem} domain model
 * via {@link com.igoy86.nexttranslate.data.mapper.CollectionMapper}.</p>
 */
public class CollectionWithWordCount {

    /**
     * All fields from the {@code collections} table row.
     * Embedded so Room maps each column automatically.
     */
    @Embedded
    public CollectionEntity collection;

    /**
     * The total number of words saved in this collection.
     * Derived via {@code COALESCE(COUNT(w.id), 0)} in the SQL query,
     * so it is always {@code 0} for empty collections rather than {@code null}.
     */
    @ColumnInfo(name = "wordCount")
    public int wordCount;
}
