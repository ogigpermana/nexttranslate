package com.igoy86.nexttranslate.data.mapper;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.local.entity.CollectionEntity;
import com.igoy86.nexttranslate.domain.model.CollectionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper utility class responsible for converting between
 * {@link CollectionEntity} (data layer) and {@link CollectionItem} (domain layer).
 *
 * <p>Mappers are a critical part of Clean Architecture — they prevent
 * data layer implementation details (Room entities) from leaking into
 * the domain or presentation layers.</p>
 *
 * <p>This class is stateless and all methods are static. It should
 * never be instantiated.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     // Entity → Domain (with word count from JOIN query)
 *     CollectionItem item = CollectionMapper.toDomain(entity, wordCount);
 *
 *     // Domain → Entity
 *     CollectionEntity entity = CollectionMapper.toEntity(item);
 * </pre>
 */
public class CollectionMapper {

    /**
     * Private constructor to prevent instantiation.
     * All methods in this class are static utility methods.
     */
    private CollectionMapper() {
        throw new UnsupportedOperationException(
                "CollectionMapper is a utility class and cannot be instantiated."
        );
    }

    // -------------------------------------------------------------------------
    // Entity → Domain
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link CollectionEntity} from the data layer into a
     * {@link CollectionItem} domain model, including its word count.
     *
     * @param entity    the Room entity to convert; must not be null
     * @param wordCount the number of words saved in this collection
     * @return the corresponding {@link CollectionItem} domain model
     */
    @NonNull
    public static CollectionItem toDomain(
            @NonNull CollectionEntity entity,
            int wordCount
    ) {
        return new CollectionItem(
                entity.getId(),
                entity.getName(),
                entity.getColorHex(),
                entity.getCreatedAt(),
                wordCount
        );
    }

    /**
     * Converts a {@link CollectionEntity} from the data layer into a
     * {@link CollectionItem} domain model with {@code wordCount} defaulting to {@code 0}.
     *
     * <p>Use this overload only when word count is not available or not needed.</p>
     *
     * @param entity the Room entity to convert; must not be null
     * @return the corresponding {@link CollectionItem} domain model with wordCount=0
     */
    @NonNull
    public static CollectionItem toDomain(@NonNull CollectionEntity entity) {
        return toDomain(entity, 0);
    }

    /**
     * Converts a list of {@link CollectionEntity} objects from the data layer
     * into a list of {@link CollectionItem} domain models, each with
     * {@code wordCount} defaulting to {@code 0}.
     *
     * <p>Returns an empty list if the input list is null or empty.</p>
     *
     * @param entities the list of Room entities to convert; may be null
     * @return a non-null list of {@link CollectionItem} domain models
     */
    @NonNull
    public static List<CollectionItem> toDomainList(List<CollectionEntity> entities) {
        final List<CollectionItem> items = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            return items;
        }
        for (CollectionEntity entity : entities) {
            items.add(toDomain(entity, 0));
        }
        return items;
    }

    // -------------------------------------------------------------------------
    // Domain → Entity
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link CollectionItem} domain model into a
     * {@link CollectionEntity} suitable for insertion into the Room database.
     *
     * <p>Note: The {@code id} field is intentionally omitted from the
     * constructor call since Room auto-generates it on insertion.
     * The {@code wordCount} field is not persisted — it is derived at
     * query time via a LEFT JOIN.</p>
     *
     * @param item the domain model to convert; must not be null
     * @return the corresponding {@link CollectionEntity} for database insertion
     */
    @NonNull
    public static CollectionEntity toEntity(@NonNull CollectionItem item) {
        return new CollectionEntity(
                item.getName(),
                item.getColorHex(),
                item.getCreatedAt()
        );
    }
}
