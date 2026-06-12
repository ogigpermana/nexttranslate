package com.igoy86.nexttranslate.presentation.collection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemDefinitionBinding;
import com.igoy86.nexttranslate.domain.model.DictionaryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that displays a numbered list of definitions for a
 * looked-up dictionary word inside the dictionary result card on
 * {@link CollectionFragment}.
 *
 * <p>Each row shows a numbered definition and, if available, a single
 * example sentence in italic below it.</p>
 *
 * <p>The maximum number of displayed definitions is capped at
 * {@value #MAX_DEFINITIONS} to keep the card compact.</p>
 */
public class DefinitionAdapter
        extends RecyclerView.Adapter<DefinitionAdapter.DefinitionViewHolder> {

    /** Maximum number of definitions shown in the card to avoid an overly long list. */
    private static final int MAX_DEFINITIONS = 5;

    /** The list of senses (definitions) to display. */
    @NonNull
    private List<DictionaryEntry.Sense> senses = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Replaces the current list of definitions with the provided senses.
     *
     * <p>Automatically caps the list at {@value #MAX_DEFINITIONS} entries.</p>
     *
     * @param newSenses the new list of {@link DictionaryEntry.Sense} objects; may be null
     */
    public void setSenses(List<DictionaryEntry.Sense> newSenses) {
        if (newSenses == null || newSenses.isEmpty()) {
            senses = new ArrayList<>();
        } else {
            senses = newSenses.subList(
                    0,
                    Math.min(newSenses.size(), MAX_DEFINITIONS)
            );
        }
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // RecyclerView.Adapter overrides
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public DefinitionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        final ItemDefinitionBinding binding = ItemDefinitionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new DefinitionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DefinitionViewHolder holder,
            int position
    ) {
        holder.bind(position + 1, senses.get(position));
    }

    @Override
    public int getItemCount() {
        return senses.size();
    }

    // =========================================================================
    // ViewHolder
    // =========================================================================

    /**
     * ViewHolder for a single definition row.
     */
    static class DefinitionViewHolder extends RecyclerView.ViewHolder {

        /** ViewBinding for the definition row layout. */
        @NonNull
        private final ItemDefinitionBinding binding;

        /**
         * Constructs a new {@link DefinitionViewHolder}.
         *
         * @param binding the inflated row ViewBinding; must not be null
         */
        DefinitionViewHolder(@NonNull ItemDefinitionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a {@link DictionaryEntry.Sense} to this row.
         *
         * @param index the 1-based display index (e.g. 1, 2, 3)
         * @param sense the sense data to display; must not be null
         */
        void bind(int index, @NonNull DictionaryEntry.Sense sense) {
            // Numbered index
            binding.textViewIndex.setText(index + ".");

            // Definition text
            binding.textViewDefinition.setText(sense.getDefinition());

            // Example sentence — show first available example if any
            if (!sense.getExamples().isEmpty()) {
                binding.textViewExample.setText(
                        "\"" + sense.getExamples().get(0) + "\""
                );
                binding.textViewExample.setVisibility(View.VISIBLE);
            } else {
                binding.textViewExample.setVisibility(View.GONE);
            }
        }
    }
}
