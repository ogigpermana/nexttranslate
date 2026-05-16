package com.igoy86.nexttranslate.data.mapper;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.local.entity.FavoriteEntity;
import com.igoy86.nexttranslate.domain.model.FavoriteItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper utility class responsible for converting between
 * {@link FavoriteEntity} (data layer) and {@link FavoriteItem} (domain layer).
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
 *     FavoriteItem item = FavoriteMapper.toDomain(entity);
 *
 *     // Domain → Entity
 *     FavoriteEntity entity = FavoriteMapper.toEntity(item);
 *
 *     // List of entities → List of domain models
 *     List{@literal <}FavoriteItem{@literal >} items = FavoriteMapper.toDomainList(entities);
 * </pre>
 */
public class FavoriteMapper {

    /**
     * Private constructor to prevent instantiation.
     * All methods in this class are static utility methods.
     */
    private FavoriteMapper() {
        throw new UnsupportedOperationException(
                "FavoriteMapper is a utility class and cannot be instantiated."
        );
    }

    // -------------------------------------------------------------------------
    // Entity → Domain
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link FavoriteEntity} from the data layer into a
     * {@link FavoriteItem} domain model.
     *
     * @param entity the Room entity to convert; must not be null
     * @return the corresponding {@link FavoriteItem} domain model
     */
    @NonNull
    public static FavoriteItem toDomain(@NonNull FavoriteEntity entity) {
        return new FavoriteItem(
                entity.getId(),
                entity.getSourceText(),
                entity.getTranslatedText(),
                entity.getSourceLanguageCode(),
                entity.getTargetLanguageCode(),
                entity.getSavedAt()
        );
    }

    /**
     * Converts a list of {@link FavoriteEntity} objects from the data layer
     * into a list of {@link FavoriteItem} domain models.
     *
     * <p>Returns an empty list if the input list is null or empty.</p>
     *
     * @param entities the list of Room entities to convert; may be null
     * @return a non-null list of {@link FavoriteItem} domain models
     */
    @NonNull
    public static List<FavoriteItem> toDomainList(List<FavoriteEntity> entities) {
        final List<FavoriteItem> items = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            return items;
        }
        for (FavoriteEntity entity : entities) {
            items.add(toDomain(entity));
        }
        return items;
    }

    // -------------------------------------------------------------------------
    // Domain → Entity
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link FavoriteItem} domain model into a {@link FavoriteEntity}
     * suitable for insertion into the Room database.
     *
     * <p>Note: The {@code id} field is intentionally omitted from the
     * constructor call since Room auto-generates it on insertion. The entity's
     * {@code id} will default to {@code 0}, which Room treats as
     * "generate a new ID".</p>
     *
     * @param item the domain model to convert; must not be null
     * @return the corresponding {@link FavoriteEntity} for database insertion
     */
    @NonNull
    public static FavoriteEntity toEntity(@NonNull FavoriteItem item) {
        return new FavoriteEntity(
                item.getSourceText(),
                item.getTranslatedText(),
                item.getSourceLanguageCode(),
                item.getTargetLanguageCode(),
                item.getSavedAt()
        );
    }
}