package com.igoy86.nexttranslate.presentation.history;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemHistoryBinding;
import com.igoy86.nexttranslate.domain.model.HistoryItem;

import com.igoy86.nexttranslate.util.TimeFormatter;

/**
 * RecyclerView adapter for displaying translation history entries.
 *
 * <p>Extends {@link ListAdapter} with {@link DiffUtil} for efficient,
 * automatic list diffing and minimal UI updates when the history list changes.</p>
 *
 * <p>Design: Gmail inbox style — items share one rounded surface container;
 * individual items have no card elevation. Top and bottom corners are rounded
 * only on the first and last items respectively via {@code updateItemShape()}.</p>
 *
 * <p>Delete is triggered by swiping left or right (handled in HistoryFragment
 * via {@code ItemTouchHelper}). The delete icon has been removed from the item
 * layout in favour of the swipe gesture.</p>
 *
 * <p>Supports two user interactions via {@link OnHistoryItemClickListener}:</p>
 * <ul>
 *     <li>Tap item — re-uses the translation on the translate screen</li>
 *     <li>Swipe left/right — triggers delete with Undo Snackbar in HistoryFragment</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     HistoryAdapter adapter = new HistoryAdapter(item -> {
 *         // handle item tap — restore translation
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
     * Callback interface for handling tap interactions on history list items.
     * Delete is handled separately via ItemTouchHelper swipe in HistoryFragment.
     */
    public interface OnHistoryItemClickListener {

        /**
         * Called when the user taps on a history item to reuse it.
         *
         * @param item the tapped {@link HistoryItem}
         */
        void onItemClick(@NonNull HistoryItem item);
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

    /** Listener for item tap events. */
    @NonNull
    private final OnHistoryItemClickListener listener;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link HistoryAdapter} with the given click listener.
     *
     * @param listener the callback for item tap; must not be null
     */
    public HistoryAdapter(@NonNull OnHistoryItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        // Enable stable IDs so ItemAnimator can animate swipe-delete correctly.
        setHasStableIds(true);
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    /**
     * Returns the database ID of the item at {@code position} as the stable ID.
     * Required because {@link #setHasStableIds(boolean)} is set to {@code true}.
     *
     * @param position the adapter position
     * @return the stable unique ID for the item
     */
    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

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
     * {@link HistoryViewHolder} and updates item corner shape for
     * Gmail-style rounded top/bottom edges.
     *
     * @param holder   the ViewHolder to update
     * @param position the position of the item in the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
        updateItemShape(holder, position);
    }
	
	@Override
    public void onCurrentListChanged(
            @NonNull java.util.List<HistoryItem> previousList,
            @NonNull java.util.List<HistoryItem> currentList
    ) {
        super.onCurrentListChanged(previousList, currentList);
        // After delete, the new last item needs its shape refreshed
        // so bottom corners become rounded again.
        if (!currentList.isEmpty()) {
            notifyItemChanged(currentList.size() - 1);
        }
        // Also refresh the new first item in case top changed
        if (!currentList.isEmpty()) {
            notifyItemChanged(0);
        }
    }

    // -------------------------------------------------------------------------
    // Public helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link HistoryItem} at the given adapter position.
     * Exposed publicly so {@link HistoryFragment} can retrieve the item
     * after a swipe gesture via {@code ItemTouchHelper}.
     *
     * @param position the adapter position of the item
     * @return the {@link HistoryItem} at that position
     */
    @NonNull
    public HistoryItem getItemAt(int position) {
        return getItem(position);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies rounded corners to the item background based on its position
     * in the list — Gmail inbox style:
     * <ul>
     *     <li>First item only → top corners rounded</li>
     *     <li>Last item only  → bottom corners rounded</li>
     *     <li>Single item     → all corners rounded</li>
     *     <li>Middle items    → no rounded corners (square)</li>
     * </ul>
     *
     * <p>Uses a {@link com.google.android.material.shape.ShapeAppearanceModel}
     * applied to the root {@link com.google.android.material.shape.MaterialShapeDrawable}
     * so the ripple foreground still clips correctly.</p>
     *
     * @param holder   the ViewHolder whose shape should be updated
     * @param position the adapter position of the item
     */
    private void updateItemShape(
            @NonNull HistoryViewHolder holder,
            int position
    ) {
        final boolean isFirst = position == 0;
        final boolean isLast  = position == getItemCount() - 1;
        final float corner    = holder.itemView.getContext()
                .getResources().getDisplayMetrics().density * 14; // 14dp

        final float topLeft     = isFirst ? corner : 0f;
        final float topRight    = isFirst ? corner : 0f;
        final float bottomLeft  = isLast  ? corner : 0f;
        final float bottomRight = isLast  ? corner : 0f;

        com.google.android.material.shape.ShapeAppearanceModel shapeModel =
                com.google.android.material.shape.ShapeAppearanceModel.builder()
                        .setTopLeftCornerSize(topLeft)
                        .setTopRightCornerSize(topRight)
                        .setBottomLeftCornerSize(bottomLeft)
                        .setBottomRightCornerSize(bottomRight)
                        .build();

        com.google.android.material.shape.MaterialShapeDrawable shapeDrawable =
                new com.google.android.material.shape.MaterialShapeDrawable(shapeModel);

        // Fill with surface container colour
        shapeDrawable.setFillColor(
                android.content.res.ColorStateList.valueOf(
                        com.google.android.material.color.MaterialColors.getColor(
                                holder.itemView,
                                com.google.android.material.R.attr.colorSurfaceContainer,
                                0
                        )
                )
        );

        holder.binding.rootItemHistory.setBackground(shapeDrawable);
        holder.binding.viewDivider.setVisibility(
                isLast ? android.view.View.GONE : android.view.View.VISIBLE);
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
        final ItemHistoryBinding binding;

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
         * and attaches a click listener for the item tap event.
         *
         * <p>Timestamp is formatted as a relative string (e.g. "2 hours ago")
         * using {@link DateUtils#getRelativeTimeSpanString}.</p>
         *
         * @param item     the history entry to display; must not be null
         * @param listener the callback for user tap; must not be null
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

            // Relative timestamp e.g. "2 hours ago"
            binding.textViewTimestamp.setText(TimeFormatter.format(item.getTimestamp()));

            // Item tap — reuse translation
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}