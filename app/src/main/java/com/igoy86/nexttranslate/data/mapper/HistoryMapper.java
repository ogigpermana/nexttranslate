package com.igoy86.nexttranslate.data.mapper;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.data.local.entity.HistoryEntity;
import com.igoy86.nexttranslate.domain.model.HistoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper utility class responsible for converting between
 * {@link HistoryEntity} (data layer) and {@link HistoryItem} (domain layer).
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
 *     HistoryItem item = HistoryMapper.toDomain(entity);
 *
 *     // Domain → Entity
 *     HistoryEntity entity = HistoryMapper.toEntity(item);
 *
 *     // List of entities → List of domain models
 *     List{@literal <}HistoryItem{@literal >} items = HistoryMapper.toDomainList(entities);
 * </pre>
 */
public class HistoryMapper {

    /**
     * Private constructor to prevent instantiation.
     * All methods in this class are static utility methods.
     */
    private HistoryMapper() {
        throw new UnsupportedOperationException(
                "HistoryMapper is a utility class and cannot be instantiated."
        );
    }

    // -------------------------------------------------------------------------
    // Entity → Domain
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link HistoryEntity} from the data layer into a
     * {@link HistoryItem} domain model.
     *
     * @param entity the Room entity to convert; must not be null
     * @return the corresponding {@link HistoryItem} domain model
     */
    @NonNull
    public static HistoryItem toDomain(@NonNull HistoryEntity entity) {
        return new HistoryItem(
                entity.getId(),
                entity.getSourceText(),
                entity.getTranslatedText(),
                entity.getSourceLanguageCode(),
                entity.getTargetLanguageCode(),
                entity.getTimestamp()
        );
    }

    /**
     * Converts a list of {@link HistoryEntity} objects from the data layer
     * into a list of {@link HistoryItem} domain models.
     *
     * <p>Returns an empty list if the input list is null or empty.</p>
     *
     * @param entities the list of Room entities to convert; may be null
     * @return a non-null list of {@link HistoryItem} domain models
     */
    @NonNull
    public static List<HistoryItem> toDomainList(List<HistoryEntity> entities) {
        final List<HistoryItem> items = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            return items;
        }
        for (HistoryEntity entity : entities) {
            items.add(toDomain(entity));
        }
        return items;
    }

    // -------------------------------------------------------------------------
    // Domain → Entity
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link HistoryItem} domain model into a {@link HistoryEntity}
     * suitable for insertion into the Room database.
     *
     * <p>Note: The {@code id} field is intentionally omitted from the
     * constructor call since Room auto-generates it on insertion. The entity's
     * {@code id} will default to {@code 0}, which Room treats as
     * "generate a new ID".</p>
     *
     * @param item the domain model to convert; must not be null
     * @return the corresponding {@link HistoryEntity} for database insertion
     */
    @NonNull
    public static HistoryEntity toEntity(@NonNull HistoryItem item) {
        return new HistoryEntity(
                item.getSourceText(),
                item.getTranslatedText(),
                item.getSourceLanguageCode(),
                item.getTargetLanguageCode(),
                item.getTimestamp()
        );
    }
}