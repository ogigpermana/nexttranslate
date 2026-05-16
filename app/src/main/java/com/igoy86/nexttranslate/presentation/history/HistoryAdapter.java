package com.igoy86.nexttranslate.presentation.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemHistoryBinding;
import com.igoy86.nexttranslate.domain.model.HistoryItem;

/**
 * RecyclerView adapter for displaying translation history entries.
 *
 * <p>Extends {@link ListAdapter} with {@link DiffUtil} for efficient,
 * automatic list diffing and minimal UI updates when the history list changes.</p>
 *
 * <p>Each item displays the source text, translated text, and language pair.
 * Supports two user interactions via {@link OnHistoryItemClickListener}:</p>
 * <ul>
 *     <li>Tap item — re-uses the translation on the translate screen</li>
 *     <li>Tap delete icon — removes the entry from history</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     HistoryAdapter adapter = new HistoryAdapter(new HistoryAdapter.OnHistoryItemClickListener() {
 *         {@literal @}Override
 *         public void onItemClick(HistoryItem item) {
 *             // handle item tap
 *         }
 *
 *         {@literal @}Override
 *         public void onDeleteClick(HistoryItem item) {
 *             viewModel.deleteHistory(item.getId());
 *         }
 *     });
 *     recyclerView.setAdapter(adapter);
 *     adapter.submitList(historyList);
 * </pre>
 */
public class HistoryAdapter extends ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder> {

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Callback interface for handling user interactions with history list items.
     */
    public interface OnHistoryItemClickListener {

        /**
         * Called when the user taps on a history item to reuse it.
         *
         * @param item the tapped {@link HistoryItem}
         */
        void onItemClick(@NonNull HistoryItem item);

        /**
         * Called when the user taps the delete icon on a history item.
         *
         * @param item the {@link HistoryItem} to delete
         */
        void onDeleteClick(@NonNull HistoryItem item);
    }

    // -------------------------------------------------------------------------
    // DiffUtil callback
    // -------------------------------------------------------------------------

    /**
     * {@link DiffUtil.ItemCallback} implementation for {@link HistoryItem}.
     *
     * <p>Used by {@link ListAdapter} to compute the minimal set of changes
     * between two lists, enabling efficient RecyclerView animations.</p>
     */
    private static final DiffUtil.ItemCallback<HistoryItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<HistoryItem>() {

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
                        @NonNull HistoryItem oldItem,
                        @NonNull HistoryItem newItem
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
                        @NonNull HistoryItem oldItem,
                        @NonNull HistoryItem newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    /** Listener for item tap and delete tap events. */
    @NonNull
    private final OnHistoryItemClickListener listener;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link HistoryAdapter} with the given click listener.
     *
     * @param listener the callback for item interactions; must not be null
     */
    public HistoryAdapter(@NonNull OnHistoryItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    /**
     * Inflates the item layout and creates a new {@link HistoryViewHolder}.
     *
     * @param parent   the RecyclerView into which the new view will be added
     * @param viewType the view type of the new view (unused; single view type)
     * @return a new {@link HistoryViewHolder} holding the inflated item view
     */
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new HistoryViewHolder(binding);
    }

    /**
     * Binds the {@link HistoryItem} at the given position to the
     * {@link HistoryViewHolder}.
     *
     * @param holder   the ViewHolder to update
     * @param position the position of the item in the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    /**
     * ViewHolder for a single translation history list item.
     *
     * <p>Holds a reference to the inflated {@link ItemHistoryBinding} and
     * binds {@link HistoryItem} data to the corresponding views.</p>
     */
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {

        /** ViewBinding for the history item layout. */
        @NonNull
        private final ItemHistoryBinding binding;

        /**
         * Constructs a new {@link HistoryViewHolder} with the given binding.
         *
         * @param binding the inflated item layout binding; must not be null
         */
        public HistoryViewHolder(@NonNull ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds the given {@link HistoryItem} data to this ViewHolder's views
         * and attaches click listeners for item tap and delete tap events.
         *
         * @param item     the history entry to display; must not be null
         * @param listener the callback for user interactions; must not be null
         */
        public void bind(
                @NonNull HistoryItem item,
                @NonNull OnHistoryItemClickListener listener
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

            // Delete icon tap — remove from history
            binding.imageViewDelete.setOnClickListener(v -> listener.onDeleteClick(item));
        }
    }
}