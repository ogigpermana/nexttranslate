package com.igoy86.nexttranslate.presentation.favorite;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemFavoriteBinding;
import com.igoy86.nexttranslate.domain.model.FavoriteItem;

import com.igoy86.nexttranslate.util.TimeFormatter;

/**
 * RecyclerView adapter for displaying favorite translation entries.
 *
 * <p>Gmail inbox style — items share one rounded surface container;
 * individual items have no card elevation. Top and bottom corners are
 * rounded only on the first and last items via {@code updateItemShape()}.</p>
 *
 * <p>Delete is triggered by swiping left or right (handled in FavoriteFragment
 * via {@code ItemTouchHelper}). No delete icon in the item layout.</p>
 *
 * <p>Tap item → {@link OnFavoriteItemClickListener#onItemClick} to restore
 * the translation on the translate screen.</p>
 */
public class FavoriteAdapter extends ListAdapter<FavoriteItem, FavoriteAdapter.FavoriteViewHolder> {

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Callback for tap interactions on favorite list items.
     * Delete is handled separately via ItemTouchHelper swipe.
     */
    public interface OnFavoriteItemClickListener {

        /**
         * Called when the user taps a favorite item to reuse it.
         *
         * @param item the tapped {@link FavoriteItem}
         */
        void onItemClick(@NonNull FavoriteItem item);
    }

    // -------------------------------------------------------------------------
    // DiffUtil
    // -------------------------------------------------------------------------

    private static final DiffUtil.ItemCallback<FavoriteItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FavoriteItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull FavoriteItem oldItem, @NonNull FavoriteItem newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull FavoriteItem oldItem, @NonNull FavoriteItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    private final OnFavoriteItemClickListener listener;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link FavoriteAdapter}.
     *
     * @param listener the callback for item tap; must not be null
     */
    public FavoriteAdapter(@NonNull OnFavoriteItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        setHasStableIds(true);
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FavoriteViewHolder(ItemFavoriteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
        updateItemShape(holder, position);
    }

    /**
     * After a delete, the new last/first items need their shapes refreshed
     * so rounded corners are applied correctly.
     */
    @Override
    public void onCurrentListChanged(
            @NonNull java.util.List<FavoriteItem> previousList,
            @NonNull java.util.List<FavoriteItem> currentList
    ) {
        super.onCurrentListChanged(previousList, currentList);
        if (!currentList.isEmpty()) {
            notifyItemChanged(0);
            notifyItemChanged(currentList.size() - 1);
        }
    }

    // -------------------------------------------------------------------------
    // Public helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the item at the given position.
     * Used by FavoriteFragment after a swipe gesture.
     */
    @NonNull
    public FavoriteItem getItemAt(int position) {
        return getItem(position);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies rounded corners based on position — Gmail inbox style.
     * Also hides the divider on the last item.
     *
     * @param holder   the ViewHolder to update
     * @param position the adapter position
     */
    private void updateItemShape(@NonNull FavoriteViewHolder holder, int position) {
        final boolean isFirst = position == 0;
        final boolean isLast  = position == getItemCount() - 1;
        final float corner    = holder.itemView.getContext()
                .getResources().getDisplayMetrics().density * 14;

        com.google.android.material.shape.ShapeAppearanceModel shapeModel =
                com.google.android.material.shape.ShapeAppearanceModel.builder()
                        .setTopLeftCornerSize(isFirst ? corner : 0f)
                        .setTopRightCornerSize(isFirst ? corner : 0f)
                        .setBottomLeftCornerSize(isLast ? corner : 0f)
                        .setBottomRightCornerSize(isLast ? corner : 0f)
                        .build();

        com.google.android.material.shape.MaterialShapeDrawable shapeDrawable =
                new com.google.android.material.shape.MaterialShapeDrawable(shapeModel);

        shapeDrawable.setFillColor(
                android.content.res.ColorStateList.valueOf(
                        com.google.android.material.color.MaterialColors.getColor(
                                holder.itemView,
                                com.google.android.material.R.attr.colorSurfaceContainer, 0)));

        holder.binding.rootItemFavorite.setBackground(shapeDrawable);

        // Hide divider on last item
        holder.binding.viewDivider.setVisibility(
                isLast ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    /**
     * ViewHolder for a single favorite translation list item.
     */
    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        final ItemFavoriteBinding binding;

        public FavoriteViewHolder(@NonNull ItemFavoriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds {@link FavoriteItem} data to the views.
         *
         * @param item     the favorite entry to display
         * @param listener the callback for item tap
         */
        public void bind(
                @NonNull FavoriteItem item,
                @NonNull OnFavoriteItemClickListener listener
        ) {
            binding.textViewSourceText.setText(item.getSourceText());
            binding.textViewTranslatedText.setText(item.getTranslatedText());
            binding.textViewLanguagePair.setText(
                    item.getSourceLanguageCode().toUpperCase()
                            + " → "
                            + item.getTargetLanguageCode().toUpperCase());

            // Relative timestamp using savedAt field
            binding.textViewTimestamp.setText(TimeFormatter.format(item.getSavedAt()));

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}