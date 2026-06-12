package com.igoy86.nexttranslate.presentation.translate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.R;
import com.igoy86.nexttranslate.domain.model.HistoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the horizontal history chip row on
 * {@link TranslateFragment}.
 *
 * <p>Displays up to {@link #MAX_ITEMS} recent history entries as
 * horizontal card chips. Each chip shows the source text (bold) and
 * the translated text below it — identical to Yandex Translate's
 * recent-history row.</p>
 *
 * <p>Tapping a chip invokes {@link OnChipClickListener#onChipClick(HistoryItem)}
 * so the Fragment can load the tapped entry back into the input field.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *     HistoryChipAdapter adapter = new HistoryChipAdapter(item -> {
 *         // load item.getSourceText() into EditText
 *     });
 *     recyclerHistory.setAdapter(adapter);
 *     adapter.submitList(historyItems); // max 5 shown automatically
 * </pre>
 */
public class HistoryChipAdapter extends RecyclerView.Adapter<HistoryChipAdapter.ChipViewHolder> {

    /** Maximum number of chips displayed in the horizontal row. */
    private static final int MAX_ITEMS = 5;

    /** The current list of history items to display (already capped at MAX_ITEMS). */
    @NonNull
    private List<HistoryItem> items = new ArrayList<>();

    /** Callback invoked when the user taps a history chip. */
    @Nullable
    private final OnChipClickListener listener;

    /**
     * Callback interface for chip tap events.
     */
    public interface OnChipClickListener {
        /**
         * Called when the user taps a history chip.
         *
         * @param item the {@link HistoryItem} associated with the tapped chip
         */
        void onChipClick(@NonNull HistoryItem item);
    }

    /**
     * Constructs a new {@link HistoryChipAdapter}.
     *
     * @param listener callback for chip tap events; may be null if no action needed
     */
    public HistoryChipAdapter(@Nullable OnChipClickListener listener) {
        this.listener = listener;
    }

    /**
     * Submits a new list of history items to the adapter.
     *
     * <p>Only the first {@link #MAX_ITEMS} entries are retained.
     * The adapter calls {@link #notifyDataSetChanged()} after updating.</p>
     *
     * @param newItems the full history list from the database; may be null or empty
     */
    public void submitList(@Nullable List<HistoryItem> newItems) {
        items = new ArrayList<>();
        if (newItems != null) {
            final int count = Math.min(newItems.size(), MAX_ITEMS);
            for (int i = 0; i < count; i++) {
                items.add(newItems.get(i));
            }
        }
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // RecyclerView.Adapter overrides
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_chip, parent, false);
        return new ChipViewHolder(view);
    }

    /** {@inheritDoc} */
    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        final HistoryItem item = items.get(position);
        holder.tvSource.setText(item.getSourceText());
        holder.tvResult.setText(item.getTranslatedText());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChipClick(item);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public int getItemCount() {
        return items.size();
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    /**
     * ViewHolder for a single history chip item.
     *
     * <p>Holds references to the source text and translated text
     * TextViews defined in {@code item_history_chip.xml}.</p>
     */
    static class ChipViewHolder extends RecyclerView.ViewHolder {

        /** TextView showing the original source text (bold). */
        final TextView tvSource;

        /** TextView showing the translated result text. */
        final TextView tvResult;

        /**
         * Constructs a new {@link ChipViewHolder} from the given item view.
         *
         * @param itemView the inflated chip item view; must not be null
         */
        ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSource = itemView.findViewById(R.id.tvHistorySource);
            tvResult = itemView.findViewById(R.id.tvHistoryResult);
        }
    }
}
