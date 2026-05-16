package com.igoy86.nexttranslate.presentation.favorite;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemFavoriteBinding;
import com.igoy86.nexttranslate.domain.model.FavoriteItem;

/**
 * RecyclerView adapter for displaying favorite translation entries.
 *
 * <p>Extends {@link ListAdapter} with {@link DiffUtil} for efficient,
 * automatic list diffing and minimal UI updates when the favorites list changes.</p>
 *
 * <p>Each item displays the source text, translated text, and language pair.
 * Supports two user interactions via {@link OnFavoriteItemClickListener}:</p>
 * <ul>
 *     <li>Tap item — re-uses the translation on the translate screen</li>
 *     <li>Tap delete icon — removes the entry from favorites</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     FavoriteAdapter adapter = new FavoriteAdapter(new FavoriteAdapter.OnFavoriteItemClickListener() {
 *         {@literal @}Override
 *         public void onItemClick(FavoriteItem item) {
 *             // handle item tap
 *         }
 *
 *         {@literal @}Override
 *         public void onDeleteClick(FavoriteItem item) {
 *             viewModel.deleteFavorite(item.getId());
 *         }
 *     });
 *     recyclerView.setAdapter(adapter);
 *     adapter.submitList(favoriteList);
 * </pre>
 */
public class FavoriteAdapter extends ListAdapter<FavoriteItem, FavoriteAdapter.FavoriteViewHolder> {

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Callback interface for handling user interactions with favorite list items.
     */
    public interface OnFavoriteItemClickListener {

        /**
         * Called when the user taps on a favorite item to reuse it.
         *
         * @param item the tapped {@link FavoriteItem}
         */
        void onItemClick(@NonNull FavoriteItem item);

        /**
         * Called when the user taps the delete icon on a favorite item.
         *
         * @param item the {@link FavoriteItem} to delete
         */
        void onDeleteClick(@NonNull FavoriteItem item);
    }

    // -------------------------------------------------------------------------
    // DiffUtil callback
    // -------------------------------------------------------------------------

    /**
     * {@link DiffUtil.ItemCallback} implementation for {@link FavoriteItem}.
     *
     * <p>Used by {@link ListAdapter} to compute the minimal set of changes
     * between two lists, enabling efficient RecyclerView animations.</p>
     */
    private static final DiffUtil.ItemCallback<FavoriteItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FavoriteItem>() {

                /**
                 * Checks whether two items represent the same database record
                 * by comparing their unique IDs.
                 *
                 * @param oldItem the item from the previous list
                 * @param newItem the item from the new list
                 * @return {@code true} if both items have the same database ID
                 */
                @Override
                public boolean areItemsTheSame(
                        @NonNull FavoriteItem oldItem,
                        @NonNull FavoriteItem newItem
                ) {
                    return oldItem.getId() == newItem.getId();
                }

                /**
                 * Checks whether two items have identical content.
                 * Called only when {@link #areItemsTheSame} returns {@code true}.
                 *
                 * @param oldItem the item from the previous list
                 * @param newItem the item from the new list
                 * @return {@code true} if all fields are equal
                 */
                @Override
                public boolean areContentsTheSame(
                        @NonNull FavoriteItem oldItem,
                        @NonNull FavoriteItem newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    /** Listener for item tap and delete tap events. */
    @NonNull
    private final OnFavoriteItemClickListener listener;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link FavoriteAdapter} with the given click listener.
     *
     * @param listener the callback for item interactions; must not be null
     */
    public FavoriteAdapter(@NonNull OnFavoriteItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    /**
     * Inflates the item layout and creates a new {@link FavoriteViewHolder}.
     *
     * @param parent   the RecyclerView into which the new view will be added
     * @param viewType the view type of the new view (unused; single view type)
     * @return a new {@link FavoriteViewHolder} holding the inflated item view
     */
    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final ItemFavoriteBinding binding = ItemFavoriteBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FavoriteViewHolder(binding);
    }

    /**
     * Binds the {@link FavoriteItem} at the given position to the
     * {@link FavoriteViewHolder}.
     *
     * @param holder   the ViewHolder to update
     * @param position the position of the item in the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    /**
     * ViewHolder for a single favorite translation list item.
     *
     * <p>Holds a reference to the inflated {@link ItemFavoriteBinding} and
     * binds {@link FavoriteItem} data to the corresponding views.</p>
     */
    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        /** ViewBinding for the favorite item layout. */
        @NonNull
        private final ItemFavoriteBinding binding;

        /**
         * Constructs a new {@link FavoriteViewHolder} with the given binding.
         *
         * @param binding the inflated item layout binding; must not be null
         */
        public FavoriteViewHolder(@NonNull ItemFavoriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds the given {@link FavoriteItem} data to this ViewHolder's views
         * and attaches click listeners for item tap and delete tap events.
         *
         * @param item     the favorite entry to display; must not be null
         * @param listener the callback for user interactions; must not be null
         */
        public void bind(
                @NonNull FavoriteItem item,
                @NonNull OnFavoriteItemClickListener listener
        ) {
            // Bind text data
            binding.textViewSourceText.setText(item.getSourceText());
            binding.textViewTranslatedText.setText(item.getTranslatedText());
            binding.textViewLanguagePair.setText(
                    item.getSourceLanguageCode().toUpperCase()
                            + " → "
                            + item.getTargetLanguageCode().toUpperCase()
            );

            // Item tap — reuse translation
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));

            // Delete icon tap — remove from favorites
            binding.imageViewDelete.setOnClickListener(v -> listener.onDeleteClick(item));
        }
    }
}