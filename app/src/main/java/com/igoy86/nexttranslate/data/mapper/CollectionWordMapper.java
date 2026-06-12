package com.igoy86.nexttranslate.data.mapper;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.local.entity.CollectionWordEntity;
import com.igoy86.nexttranslate.domain.model.CollectionWordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper utility class responsible for converting between
 * {@link CollectionWordEntity} (data layer) and {@link CollectionWordItem} (domain layer).
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
 *     // Entity → Domain
 *     CollectionWordItem item = CollectionWordMapper.toDomain(entity);
 *
 *     // Domain → Entity
 *     CollectionWordEntity entity = CollectionWordMapper.toEntity(item);
 *
 *     // List of entities → List of domain models
 *     List{@literal <}CollectionWordItem{@literal >} items =
 *         CollectionWordMapper.toDomainList(entities);
 * </pre>
 */
public class CollectionWordMapper {

    /**
     * Private constructor to prevent instantiation.
     * All methods in this class are static utility methods.
     */
    private CollectionWordMapper() {
        throw new UnsupportedOperationException(
                "CollectionWordMapper is a utility class and cannot be instantiated."
        );
    }

    // -------------------------------------------------------------------------
    // Entity → Domain
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link CollectionWordEntity} from the data layer into a
     * {@link CollectionWordItem} domain model.
     *
     * @param entity the Room entity to convert; must not be null
     * @return the corresponding {@link CollectionWordItem} domain model
     */
    @NonNull
    public static CollectionWordItem toDomain(@NonNull CollectionWordEntity entity) {
        return new CollectionWordItem(
                entity.getId(),
                entity.getCollectionId(),
                entity.getWord(),
                entity.getDefinition(),
                entity.getAddedAt()
        );
    }

    /**
     * Converts a list of {@link CollectionWordEntity} objects into a list of
     * {@link CollectionWordItem} domain models.
     *
     * <p>Returns an empty list if the input list is null or empty.</p>
     *
     * @param entities the list of Room entities to convert; may be null
     * @return a non-null list of {@link CollectionWordItem} domain models
     */
    @NonNull
    public static List<CollectionWordItem> toDomainList(List<CollectionWordEntity> entities) {
        final List<CollectionWordItem> items = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            return items;
        }
        for (CollectionWordEntity entity : entities) {
            items.add(toDomain(entity));
        }
        return items;
    }

    // -------------------------------------------------------------------------
    // Domain → Entity
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link CollectionWordItem} domain model into a
     * {@link CollectionWordEntity} suitable for insertion into the Room database.
     *
     * <p>Note: The {@code id} field is intentionally set to {@code 0} so that
     * Room auto-generates it on insertion.</p>
     *
     * @param item the domain model to convert; must not be null
     * @return the corresponding {@link CollectionWordEntity} for database insertion
     */
    @NonNull
    public static CollectionWordEntity toEntity(@NonNull CollectionWordItem item) {
        return new CollectionWordEntity(
                item.getCollectionId(),
                item.getWord(),
                item.getDefinition(),
                item.getAddedAt()
        );
    }
}
