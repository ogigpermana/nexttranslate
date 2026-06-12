package com.igoy86.nexttranslate.presentation.collection;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemCollectionCardBinding;
import com.igoy86.nexttranslate.domain.model.CollectionItem;

/**
 * RecyclerView adapter for the "Koleksi Saya" 2-column grid on
 * {@link CollectionFragment}.
 *
 * <p>Uses {@link ListAdapter} with {@link DiffUtil} for efficient,
 * animation-aware updates when the underlying list changes.</p>
 *
 * <p>Each card displays:</p>
 * <ul>
 *     <li>The collection name in bold white text.</li>
 *     <li>The item count at the bottom-left.</li>
 *     <li>A solid colour background derived from {@link CollectionItem#getColorHex()}.</li>
 * </ul>
 *
 * <p>Interactions:</p>
 * <ul>
 *     <li>Single tap → {@link OnCollectionClickListener#onCollectionClick}</li>
 *     <li>Long press → {@link OnCollectionClickListener#onCollectionLongClick}
 *         (triggers rename/delete popup menu in the Fragment)</li>
 * </ul>
 */
public class CollectionAdapter
        extends ListAdapter<CollectionItem, CollectionAdapter.CollectionViewHolder> {

    // -------------------------------------------------------------------------
    // Callback interface
    // -------------------------------------------------------------------------

    /**
     * Listener interface for collection card interaction events.
     */
    public interface OnCollectionClickListener {

        /**
         * Called when the user taps a collection card.
         *
         * @param item the tapped {@link CollectionItem}
         */
        void onCollectionClick(@NonNull CollectionItem item);

        /**
         * Called when the user long-presses a collection card.
         *
         * @param item the long-pressed {@link CollectionItem}
         * @return {@code true} if the event was consumed
         */
        boolean onCollectionLongClick(@NonNull CollectionItem item);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Listener for tap and long-press events on collection cards. */
    @NonNull
    private final OnCollectionClickListener listener;

    // -------------------------------------------------------------------------
    // DiffUtil.ItemCallback
    // -------------------------------------------------------------------------

    /**
     * {@link DiffUtil.ItemCallback} for efficient list diffing.
     *
     * <p>Two {@link CollectionItem} instances are considered the same item
     * if they share the same database {@code id}. Their contents are
     * considered equal if all fields match.</p>
     */
    private static final DiffUtil.ItemCallback<CollectionItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CollectionItem>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull CollectionItem oldItem,
                        @NonNull CollectionItem newItem
                ) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CollectionItem oldItem,
                        @NonNull CollectionItem newItem
                ) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getColorHex().equals(newItem.getColorHex())
                            && oldItem.getWordCount() == newItem.getWordCount();
                }
            };

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionAdapter}.
     *
     * @param listener the click/long-click listener; must not be null
     */
    public CollectionAdapter(@NonNull OnCollectionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        final ItemCollectionCardBinding binding = ItemCollectionCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CollectionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CollectionViewHolder holder,
            int position
    ) {
        holder.bind(getItem(position));
    }

    // =========================================================================
    // ViewHolder
    // =========================================================================

    /**
     * ViewHolder for a single collection card in the 2-column grid.
     */
    class CollectionViewHolder extends RecyclerView.ViewHolder {

        /** ViewBinding for the card layout. */
        @NonNull
        private final ItemCollectionCardBinding binding;

        /**
         * Constructs a new {@link CollectionViewHolder}.
         *
         * @param binding the inflated card ViewBinding; must not be null
         */
        CollectionViewHolder(@NonNull ItemCollectionCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a {@link CollectionItem} to this card.
         *
         * <p>Sets the card background colour, collection name, and item count,
         * then attaches click and long-click listeners.</p>
         *
         * @param item the collection data to display; must not be null
         */
        void bind(@NonNull CollectionItem item) {
            // Collection name
            binding.textViewCollectionName.setText(item.getName());

            // Item count — derived from LEFT JOIN with collection_words table
            binding.textViewItemCount.setText(String.valueOf(item.getWordCount()));

            // Card background colour
            try {
                binding.cardCollection.setCardBackgroundColor(
                        Color.parseColor(item.getColorHex())
                );
            } catch (IllegalArgumentException ex) {
                // Fallback to teal if colour string is malformed
                binding.cardCollection.setCardBackgroundColor(Color.parseColor("#00897B"));
            }

            // Tap — open collection detail
            binding.cardCollection.setOnClickListener(v ->
                    listener.onCollectionClick(item)
            );

            // Long press — show rename/delete menu
            binding.cardCollection.setOnLongClickListener(v ->
                    listener.onCollectionLongClick(item)
            );
        }
    }
}
