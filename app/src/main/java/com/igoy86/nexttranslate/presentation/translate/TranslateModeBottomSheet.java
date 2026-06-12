package com.igoy86.nexttranslate.presentation.translate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.igoy86.nexttranslate.R;

/**
 * Bottom sheet dialog for selecting the translation mode.
 *
 * <p>Presents two modes to the user:</p>
 * <ul>
 *     <li><b>Fast</b> — offline translation using ML Kit (on-device)</li>
 *     <li><b>AI Online</b> — remote translation using Groq API</li>
 * </ul>
 *
 * <p>The current mode is reflected by a {@link MaterialSwitch}.
 * Toggling the switch notifies the caller via
 * {@link OnModeSelectedListener} and immediately dismisses the sheet.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * TranslateModeBottomSheet sheet =
 *     TranslateModeBottomSheet.newInstance(currentIsOnline);
 * sheet.setOnModeSelectedListener(isOnline -> viewModel.setOnlineMode(isOnline));
 * sheet.show(getParentFragmentManager(), TranslateModeBottomSheet.TAG);
 * </pre>
 */
public class TranslateModeBottomSheet extends BottomSheetDialogFragment {

    /** Fragment tag used when adding to the back stack. */
    public static final String TAG = "TranslateModeBottomSheet";

    /** Bundle key for the initial online-mode boolean argument. */
    private static final String ARG_IS_ONLINE = "arg_is_online";

    /**
     * Callback interface invoked when the user selects a translation mode.
     */
    public interface OnModeSelectedListener {
        /**
         * Called when the user changes the translation mode.
         *
         * @param isOnline {@code true} if AI Online (Groq) was selected;
         *                 {@code false} if Fast (ML Kit) was selected
         */
        void onModeSelected(boolean isOnline);
    }

    /** Listener to be notified when the mode selection changes. */
    @Nullable
    private OnModeSelectedListener listener;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance of {@link TranslateModeBottomSheet} with the
     * current online-mode state pre-applied to the switch.
     *
     * @param isOnline {@code true} if online mode is currently active
     * @return a configured {@link TranslateModeBottomSheet} instance
     */
    @NonNull
    public static TranslateModeBottomSheet newInstance(boolean isOnline) {
        final TranslateModeBottomSheet sheet = new TranslateModeBottomSheet();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_IS_ONLINE, isOnline);
        sheet.setArguments(args);
        return sheet;
    }

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------

    /**
     * Registers the listener to be invoked when the user selects a mode.
     *
     * @param listener the callback; pass {@code null} to clear it
     */
    public void setOnModeSelectedListener(@Nullable OnModeSelectedListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_translate_mode, container, false);
    }

    /** {@inheritDoc} */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final boolean isOnline = getArguments() != null
                && getArguments().getBoolean(ARG_IS_ONLINE, false);

        final MaterialSwitch switchMode = view.findViewById(R.id.switchTranslateMode);
        final TextView textModeFast = view.findViewById(R.id.textModeFast);
        final TextView textModeAi = view.findViewById(R.id.textModeAi);

        // Apply initial state
        switchMode.setChecked(isOnline);
        updateModeLabels(textModeFast, textModeAi, isOnline);

        // Row taps also toggle the switch
        view.findViewById(R.id.rowModeFast).setOnClickListener(v -> {
            if (switchMode.isChecked()) {
                switchMode.setChecked(false);
                notifyAndDismiss(false, textModeFast, textModeAi);
            }
        });

        view.findViewById(R.id.rowModeAi).setOnClickListener(v -> {
            if (!switchMode.isChecked()) {
                switchMode.setChecked(true);
                notifyAndDismiss(true, textModeFast, textModeAi);
            }
        });

        // Switch toggle
        switchMode.setOnCheckedChangeListener((buttonView, checked) -> {
            updateModeLabels(textModeFast, textModeAi, checked);
            notifyAndDismiss(checked, textModeFast, textModeAi);
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Updates the visual emphasis of the two mode labels to indicate which
     * option is currently active.
     *
     * @param textFast  the "Fast" mode label view
     * @param textAi    the "AI Online" mode label view
     * @param isOnline  {@code true} when AI Online is active
     */
    private void updateModeLabels(
            @NonNull TextView textFast,
            @NonNull TextView textAi,
            boolean isOnline
    ) {
        textFast.setAlpha(isOnline ? 0.45f : 1.0f);
        textAi.setAlpha(isOnline ? 1.0f : 0.45f);
    }

    /**
     * Notifies the registered {@link OnModeSelectedListener} of the selected
     * mode and then dismisses the bottom sheet.
     *
     * <p>Guards against double-invocation: once the sheet is dismissed the
     * listener reference is cleared.</p>
     *
     * @param isOnline    the newly selected mode
     * @param textFast    the "Fast" mode label (passed to avoid re-lookup)
     * @param textAi      the "AI Online" mode label
     */
    private void notifyAndDismiss(
            boolean isOnline,
            @NonNull TextView textFast,
            @NonNull TextView textAi
    ) {
        updateModeLabels(textFast, textAi, isOnline);
        if (listener != null) {
            listener.onModeSelected(isOnline);
            listener = null; // prevent double-fire on dismiss
        }
        dismiss();
    }
}
