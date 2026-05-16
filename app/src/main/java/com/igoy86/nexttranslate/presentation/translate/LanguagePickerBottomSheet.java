package com.igoy86.nexttranslate.presentation.translate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.LanguageModel;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.R;

import java.util.ArrayList;
import java.util.List;

/**
 * BottomSheetDialogFragment for picking a language from the list of
 * already-downloaded ML Kit translation models.
 *
 * <p>Launched from {@link TranslateFragment} when the user taps the
 * source or target language button. Only shows languages that have
 * been downloaded via the Language Manager.</p>
 *
 * <p>Use {@link #newInstance(String)} to create an instance, passing
 * either {@link #MODE_SOURCE} or {@link #MODE_TARGET} to indicate
 * which language slot is being changed.</p>
 */
public class LanguagePickerBottomSheet extends BottomSheetDialogFragment {

    /** Tag used for logging and FragmentManager transactions. */
    public static final String TAG = "LanguagePickerBottomSheet";

    /** Argument key for the picker mode (source or target). */
    private static final String ARG_MODE = "mode";

    /**
     * Argument key for the currently active language code.
     * Used to show a checkmark next to the currently selected language.
     */
    private static final String ARG_ACTIVE_CODE = "active_code";

    /** Mode value indicating the source language is being selected. */
    public static final String MODE_SOURCE = "source";

    /** Mode value indicating the target language is being selected. */
    public static final String MODE_TARGET = "target";

    /** Callback interface for delivering the selected language back to the caller. */
    public interface OnLanguageSelectedListener {
        /**
         * Called when the user selects a language from the list.
         *
         * @param languageCode the BCP-47 code of the selected language (e.g. "id")
         * @param mode         either {@link #MODE_SOURCE} or {@link #MODE_TARGET}
         */
        void onLanguageSelected(@NonNull String languageCode, @NonNull String mode);
    }

    /** Listener to notify when a language is selected. Set by the caller. */
    @Nullable
    private OnLanguageSelectedListener listener;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance of {@link LanguagePickerBottomSheet}.
     *
     * @param mode       either {@link #MODE_SOURCE} or {@link #MODE_TARGET}
     * @param activeCode the BCP-47 code of the currently selected language,
     *                   used to display a checkmark on the active item
     * @return a configured {@link LanguagePickerBottomSheet} instance
     */
    @NonNull
    public static LanguagePickerBottomSheet newInstance(
            @NonNull String mode,
            @NonNull String activeCode
    ) {
        final LanguagePickerBottomSheet sheet = new LanguagePickerBottomSheet();
        final Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        args.putString(ARG_ACTIVE_CODE, activeCode);
        sheet.setArguments(args);
        return sheet;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Build layout programmatically — no XML needed
        final android.widget.LinearLayout root = new android.widget.LinearLayout(requireContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(0, 32, 0, 32);

        // Title
        final TextView title = new TextView(requireContext());
        final String mode = getArguments() != null
                ? getArguments().getString(ARG_MODE, MODE_SOURCE)
                : MODE_SOURCE;
        final String activeCode = getArguments() != null
                ? getArguments().getString(ARG_ACTIVE_CODE, "")
                : "";
        title.setText(MODE_SOURCE.equals(mode) ? "Select Source Language" : "Select Target Language");
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(64, 24, 64, 24);
        root.addView(title);

        // Divider
        final View divider = new View(requireContext());
        divider.setBackgroundColor(0x1F000000);
        final android.widget.LinearLayout.LayoutParams dividerParams =
                new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
        root.addView(divider, dividerParams);

        // RecyclerView
        final RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        final android.widget.LinearLayout.LayoutParams rvParams =
                new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(recyclerView, rvParams);

        // Load downloaded languages and populate list
        final AppContainer appContainer = NextTranslateApp.getContainer();
        appContainer.getGetDownloadedLanguagesUseCase().execute()
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource == null || !resource.isSuccess() || resource.getData() == null) return;

                    // Filter only downloaded languages
                    final List<LanguageModel> downloaded = new ArrayList<>();
                    for (LanguageModel lang : resource.getData()) {
                        if (lang.isDownloaded()) {
                            downloaded.add(lang);
                        }
                    }

                    FileLogger.d(TAG, "Downloaded languages for picker: " + downloaded.size());

                    recyclerView.setAdapter(new PickerAdapter(downloaded, activeCode, selectedCode -> {
                        if (listener != null) {
                            listener.onLanguageSelected(selectedCode, mode);
                        }
                        dismiss();
                    }));
                });

        return root;
    }

    /**
     * Sets the listener to be notified when a language is selected.
     *
     * @param listener the callback; must not be null
     */
    public void setOnLanguageSelectedListener(@NonNull OnLanguageSelectedListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Internal adapter
    // -------------------------------------------------------------------------

    /**
     * Simple RecyclerView adapter for the language picker list.
     */
    private static class PickerAdapter
            extends RecyclerView.Adapter<PickerAdapter.ViewHolder> {

        interface OnPickListener {
            void onPick(@NonNull String languageCode);
        }

        @NonNull
        private final List<LanguageModel> items;

        @NonNull
        private final String activeCode;

        @NonNull
        private final OnPickListener onPickListener;

        PickerAdapter(
                @NonNull List<LanguageModel> items,
                @NonNull String activeCode,
                @NonNull OnPickListener listener
        ) {
            this.items = items;
            this.activeCode = activeCode;
            this.onPickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Row: horizontal layout so text stays left and checkmark goes right
            final android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(64, 36, 64, 36);
            row.setBackground(getSelectableBackground(parent.getContext()));

            // Text column (name + code stacked vertically)
            final android.widget.LinearLayout textColumn = new android.widget.LinearLayout(parent.getContext());
            textColumn.setOrientation(android.widget.LinearLayout.VERTICAL);
            final android.widget.LinearLayout.LayoutParams textParams =
                    new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textColumn.setLayoutParams(textParams);

            final TextView nameView = new TextView(parent.getContext());
            nameView.setTextSize(15f);
            textColumn.addView(nameView);

            final TextView codeView = new TextView(parent.getContext());
            codeView.setTextSize(12f);
            codeView.setAlpha(0.6f);
            textColumn.addView(codeView);

            row.addView(textColumn);

            // Checkmark icon — visible only for the active language
            final android.widget.ImageView checkView = new android.widget.ImageView(parent.getContext());
            final int sizePx = (int) (24 * parent.getContext().getResources().getDisplayMetrics().density);
            final android.widget.LinearLayout.LayoutParams checkParams =
                    new android.widget.LinearLayout.LayoutParams(sizePx, sizePx);
            checkParams.setMarginStart((int) (8 * parent.getContext().getResources().getDisplayMetrics().density));
            checkView.setLayoutParams(checkParams);
            checkView.setImageResource(R.drawable.ic_check);
            checkView.setVisibility(View.GONE);
            row.addView(checkView);

            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            return new ViewHolder(row, nameView, codeView, checkView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final LanguageModel lang = items.get(position);
            holder.nameView.setText(lang.getDisplayName());
            holder.codeView.setText(lang.getLanguageCode().toUpperCase());
            holder.checkView.setVisibility(
                    lang.getLanguageCode().equals(activeCode) ? View.VISIBLE : View.GONE
            );
            holder.itemView.setOnClickListener(v ->
                    onPickListener.onPick(lang.getLanguageCode()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static android.graphics.drawable.Drawable getSelectableBackground(
                android.content.Context context) {
            final int[] attrs = {android.R.attr.selectableItemBackground};
            final android.content.res.TypedArray ta = context.obtainStyledAttributes(attrs);
            final android.graphics.drawable.Drawable drawable = ta.getDrawable(0);
            ta.recycle();
            return drawable;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView nameView;
            final TextView codeView;
            final android.widget.ImageView checkView;

            ViewHolder(
                    @NonNull View itemView,
                    @NonNull TextView nameView,
                    @NonNull TextView codeView,
                    @NonNull android.widget.ImageView checkView
            ) {
                super(itemView);
                this.nameView = nameView;
                this.codeView = codeView;
                this.checkView = checkView;
            }
        }
    }
}
