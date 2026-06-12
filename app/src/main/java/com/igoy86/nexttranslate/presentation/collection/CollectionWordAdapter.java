package com.igoy86.nexttranslate.presentation.collection;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemCollectionWordBinding;
import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.util.TimeFormatter;

/**
 * RecyclerView adapter for the word list inside {@link CollectionDetailFragment}.
 *
 * <p>Uses {@link ListAdapter} with {@link DiffUtil} for efficient,
 * animation-aware updates when the word list changes.</p>
 *
 * <p>Each row displays:</p>
 * <ul>
 *     <li>The saved word in bold.</li>
 *     <li>A short definition in primary colour.</li>
 *     <li>A relative timestamp (e.g. "2 hours ago").</li>
 *     <li>A delete icon button to remove the word from the collection.</li>
 * </ul>
 */
public class CollectionWordAdapter
        extends ListAdapter<CollectionWordItem, CollectionWordAdapter.WordViewHolder> {

    // -------------------------------------------------------------------------
    // Callback interface
    // -------------------------------------------------------------------------

    /**
     * Listener for delete button taps on a word row.
     */
    public interface OnDeleteClickListener {
        /**
         * Called when the user taps the delete icon for a word.
         *
         * @param item the {@link CollectionWordItem} to delete
         */
        void onDeleteClick(@NonNull CollectionWordItem item);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    @NonNull
    private final OnDeleteClickListener deleteListener;

    // -------------------------------------------------------------------------
    // DiffUtil
    // -------------------------------------------------------------------------

    private static final DiffUtil.ItemCallback<CollectionWordItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CollectionWordItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CollectionWordItem oldItem,
                        @NonNull CollectionWordItem newItem
                ) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CollectionWordItem oldItem,
                        @NonNull CollectionWordItem newItem
                ) {
                    return oldItem.getWord().equals(newItem.getWord())
                            && oldItem.getDefinition().equals(newItem.getDefinition());
                }
            };

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionWordAdapter}.
     *
     * @param deleteListener listener for delete button taps; must not be null
     */
    public CollectionWordAdapter(@NonNull OnDeleteClickListener deleteListener) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final ItemCollectionWordBinding binding = ItemCollectionWordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new WordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // =========================================================================
    // ViewHolder
    // =========================================================================

    class WordViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemCollectionWordBinding binding;

        WordViewHolder(@NonNull ItemCollectionWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CollectionWordItem item) {
            binding.textViewWord.setText(item.getWord());
            binding.textViewDefinition.setText(item.getDefinition());
            binding.textViewAddedAt.setText(TimeFormatter.format(item.getAddedAt()));

            binding.btnDeleteWord.setOnClickListener(v -> deleteListener.onDeleteClick(item));
        }
    }
}
