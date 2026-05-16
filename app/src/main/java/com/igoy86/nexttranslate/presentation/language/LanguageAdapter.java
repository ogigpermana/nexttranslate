package com.igoy86.nexttranslate.presentation.language;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.igoy86.nexttranslate.databinding.ItemLanguageBinding;
import com.igoy86.nexttranslate.domain.model.DownloadProgress;
import com.igoy86.nexttranslate.domain.model.LanguageModel;

import java.util.Locale;

/**
 * RecyclerView adapter for displaying supported language models in the
 * Language Manager screen.
 *
 * <p>Extends {@link ListAdapter} with {@link DiffUtil} for efficient,
 * automatic list diffing and minimal UI updates when the language list changes.</p>
 *
 * <p>Each item displays the language display name, BCP-47 code, and download
 * status. The download/delete button is shown based on whether the model is
 * already downloaded.</p>
 *
 * <p>During a download, the item shows a horizontal {@link LinearProgressIndicator}
 * with a "14,08 MB / 57,11 MB" label — matching Google Translate's UX.
 * While waiting for the first progress event (or during delete), a circular
 * indeterminate spinner is shown instead.</p>
 *
 * <p>Supports two user interactions via {@link OnLanguageItemClickListener}:</p>
 * <ul>
 *     <li>Tap download button — triggers download preference dialog in Fragment</li>
 *     <li>Tap delete button — triggers delete confirmation dialog in Fragment</li>
 * </ul>
 */
public class LanguageAdapter extends ListAdapter<LanguageModel, LanguageAdapter.LanguageViewHolder> {

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    /**
     * Callback interface for handling user interactions with language list items.
     */
    public interface OnLanguageItemClickListener {

        /**
         * Called when the user taps the download button for a language model.
         *
         * @param model the {@link LanguageModel} to download
         */
        void onDownloadClick(@NonNull LanguageModel model);

        /**
         * Called when the user taps the delete button for a downloaded language model.
         *
         * @param model the {@link LanguageModel} to delete
         */
        void onDeleteClick(@NonNull LanguageModel model);
    }

    // -------------------------------------------------------------------------
    // DiffUtil callback
    // -------------------------------------------------------------------------

    private static final DiffUtil.ItemCallback<LanguageModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LanguageModel>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull LanguageModel oldItem,
                        @NonNull LanguageModel newItem
                ) {
                    return oldItem.getLanguageCode().equals(newItem.getLanguageCode());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull LanguageModel oldItem,
                        @NonNull LanguageModel newItem
                ) {
                    return oldItem.equals(newItem)
                            && oldItem.isDownloaded() == newItem.isDownloaded();
                }
            };

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Listener for download and delete button tap events. */
    @NonNull
    private final OnLanguageItemClickListener listener;

    /**
     * The BCP-47 language code of the model currently being downloaded or deleted.
     * {@code null} when no operation is in progress.
     */
    @Nullable
    private String activeLanguageCode;

    /**
     * The current byte-level download progress for the active download.
     * {@code null} when no download is in progress or total size is not yet known.
     */
    @Nullable
    private DownloadProgress downloadProgress;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link LanguageAdapter} with the given click listener.
     *
     * @param listener the callback for item interactions; must not be null
     */
    public LanguageAdapter(@NonNull OnLanguageItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // ListAdapter overrides
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final ItemLanguageBinding binding = ItemLanguageBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new LanguageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        final LanguageModel model = getItem(position);
        final boolean isActive = model.getLanguageCode().equals(activeLanguageCode);

        // Pass progress only to the item that is actively downloading
        final DownloadProgress progressForItem = (isActive && downloadProgress != null
                && model.getLanguageCode().equals(downloadProgress.getLanguageCode()))
                ? downloadProgress
                : null;

        holder.bind(model, isActive, progressForItem, listener);
    }

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    /**
     * Updates the active language code and refreshes the RecyclerView to
     * show or hide per-item indicators.
     *
     * @param languageCode the BCP-47 code of the currently active language,
     *                     or {@code null} if no operation is in progress
     */
    public void setActiveLanguageCode(@Nullable String languageCode) {
        this.activeLanguageCode = languageCode;
        if (languageCode == null) {
            // Clear progress when operation ends
            this.downloadProgress = null;
        }
        notifyDataSetChanged();
    }

    /**
     * Updates the download progress and refreshes the RecyclerView to
     * show real byte-level progress on the active download item.
     *
     * <p>Call this from the Fragment when observing
     * {@link LanguageViewModel#getDownloadProgressLiveData()}.</p>
     *
     * @param progress the current {@link DownloadProgress}, or {@code null} if none
     */
    public void setDownloadProgress(@Nullable DownloadProgress progress) {
        this.downloadProgress = progress;
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    /**
     * ViewHolder for a single language model list item.
     */
    public static class LanguageViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final ItemLanguageBinding binding;

        public LanguageViewHolder(@NonNull ItemLanguageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds {@link LanguageModel} data to this ViewHolder's views.
         *
         * <p>Visibility logic:</p>
         * <ul>
         *   <li>isActive + progress known → horizontal progress bar + MB text</li>
         *   <li>isActive + no progress    → circular indeterminate spinner</li>
         *   <li>not active + downloaded   → delete button</li>
         *   <li>not active + not downloaded → download button</li>
         * </ul>
         *
         * @param model    the language model to display
         * @param isActive whether this item is currently being operated on
         * @param progress byte-level progress if actively downloading, else {@code null}
         * @param listener click callback
         */
        public void bind(
                @NonNull LanguageModel model,
                boolean isActive,
                @Nullable DownloadProgress progress,
                @NonNull OnLanguageItemClickListener listener
        ) {
            binding.textViewLanguageName.setText(model.getDisplayName());
            binding.textViewLanguageCode.setText(model.getLanguageCode().toUpperCase());

            // Default languages (e.g. English) are always available and cannot be deleted.
            // Show a checkmark icon instead of a Delete button, matching Google Translate's UX.
            if (model.isDefault()) {
                binding.layoutDownloadProgress.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonDownload.setVisibility(View.GONE);
                binding.buttonDelete.setVisibility(View.GONE);
                binding.imageViewDefaultCheck.setVisibility(View.VISIBLE);
                return;
            }

            // Hide checkmark for non-default languages
            binding.imageViewDefaultCheck.setVisibility(View.GONE);

            if (isActive && progress != null && progress.getTotalBytes() > 0) {
                // ── Active download with known size: show horizontal progress ──
                binding.layoutDownloadProgress.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonDownload.setVisibility(View.GONE);
                binding.buttonDelete.setVisibility(View.GONE);

                // Update progress bar (0–100)
                binding.progressBarDownload.setProgressCompat(progress.getPercent(), true);

                // Format: "14,08 MB / 57,11 MB"
                final String mbText = formatMb(progress.getBytesDownloaded())
                        + " / " + formatMb(progress.getTotalBytes());
                binding.textViewDownloadMb.setText(mbText);

            } else if (isActive) {
                // ── Active but total size not yet known (or delete): circular spinner ──
                binding.layoutDownloadProgress.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.buttonDownload.setVisibility(View.GONE);
                binding.buttonDelete.setVisibility(View.GONE);

            } else if (model.isDownloaded()) {
                // ── Downloaded: show delete button ──
                binding.layoutDownloadProgress.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonDownload.setVisibility(View.GONE);
                binding.buttonDelete.setVisibility(View.VISIBLE);
                binding.buttonDelete.setOnClickListener(v -> listener.onDeleteClick(model));

            } else {
                // ── Not downloaded: show download button ──
                binding.layoutDownloadProgress.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonDownload.setVisibility(View.VISIBLE);
                binding.buttonDelete.setVisibility(View.GONE);
                binding.buttonDownload.setOnClickListener(v -> listener.onDownloadClick(model));
            }
        }

        /**
         * Formats a byte count as a human-readable MB string.
         * e.g. 14768742 → "14,08 MB"
         *
         * @param bytes number of bytes
         * @return formatted string like "14,08 MB"
         */
        private String formatMb(long bytes) {
            final double mb = bytes / (1024.0 * 1024.0);
            return String.format(Locale.getDefault(), "%.2f MB", mb);
        }
    }
}
