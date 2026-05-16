package com.igoy86.nexttranslate.presentation.translate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.MainActivity;
import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.R;
import com.igoy86.nexttranslate.databinding.FragmentTranslateBinding;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Fragment for the main translation screen.
 *
 * <p>Provides the primary user interface for entering text, selecting
 * source and target languages, performing translation, copying results,
 * and adding translations to favorites.</p>
 *
 * <p>Translation is triggered automatically after the user stops typing
 * for {@link #AUTO_TRANSLATE_DELAY_MS} milliseconds, with a minimum
 * input length of {@link #MIN_AUTO_TRANSLATE_LENGTH} characters.</p>
 *
 * <p>Observes {@link TranslateViewModel} for translation results,
 * loading states, detected language codes, and favorite events.</p>
 */
public class TranslateFragment extends BaseFragment<FragmentTranslateBinding> {

    /** Tag used for logging events originating from this Fragment. */
    private static final String TAG = "TranslateFragment";

    /**
     * Delay in milliseconds after the user stops typing before
     * auto-translation is triggered.
     */
    private static final long AUTO_TRANSLATE_DELAY_MS = 1000L;

    /**
     * Minimum number of characters required to trigger auto-translation
     * and language detection.
     */
    private static final int MIN_AUTO_TRANSLATE_LENGTH = 2;

    /** ViewModel managing the translation UI state. */
    private TranslateViewModel viewModel;

    /** The most recently received translation result. Used for copy and favorite actions. */
    @Nullable
    private TranslationResult currentTranslationResult;

    /** Handler used to post delayed auto-translate and detect-language runnables. */
    private final Handler autoTranslateHandler = new Handler(Looper.getMainLooper());

    /** Runnable that triggers auto-translation after the debounce delay. */
    @Nullable
    private Runnable autoTranslateRunnable;

    /**
     * Timestamp when shimmer started — used to enforce a minimum visible
     * duration so the shimmer is always noticeable to the user.
     */
    private long shimmerStartTime = 0L;

    /** Minimum time the shimmer stays visible in milliseconds. */
    private static final long MIN_SHIMMER_DURATION_MS = 600L;

    /**
     * Flag to skip auto-retranslate on first observer emission after
     * fragment recreation (e.g. back from Favorites/History).
     * Set to {@code true} after the first emission is consumed.
     */
    private boolean isFirstObserverEmit = true;

    // -------------------------------------------------------------------------
    // BaseFragment implementation
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @NonNull
    @Override
    protected FragmentTranslateBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentTranslateBinding.inflate(inflater, container, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes the {@link TranslateViewModel} and applies
     * {@link WindowInsetsCompat} padding to avoid overlap with the
     * system status bar and navigation bar.</p>
     */
    @Override
    protected void initViews() {
        final AppContainer container = NextTranslateApp.getContainer();

        final TranslateViewModelFactory factory = new TranslateViewModelFactory(
                container.getTranslateTextUseCase(),
                container.getDetectLanguageUseCase(),
                container.getAddHistoryUseCase(),
                container.getAddFavoriteUseCase()
        );

        viewModel = new ViewModelProvider(this, factory).get(TranslateViewModel.class);

        // Fix overlap with system status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().getRoot(), (v, insets) -> {
            final int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            final int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        // Inflate toolbar menu for Language Manager access
        getBinding().toolbar.inflateMenu(R.menu.menu_translate);
        getBinding().toolbar.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.id.action_favorites) {
                ((MainActivity) requireActivity()).openFavoriteFragment();
                return true;
            }
            if (item.getItemId() == R.id.action_language_manager) {
                ((MainActivity) requireActivity()).openLanguageFragment();
                return true;
            }
            return false;
        });

        FileLogger.d(TAG, "TranslateFragment views initialized.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Observes translation result, loading state, language selections,
     * error messages, and favorite events from {@link TranslateViewModel}.</p>
     */
    @Override
    protected void initObservers() {
        // Observe translation result
        viewModel.getTranslationResultLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isLoading()) {
                showLoading(true);
            } else if (resource.isSuccess() && resource.getData() != null) {
                showLoading(false);
                currentTranslationResult = resource.getData();
                getBinding().textViewTranslationResult.setText(
                        resource.getData().getTranslatedText()
                );
                FileLogger.d(TAG, "Translation result displayed.");
            } else if (resource.isError()) {
                showLoading(false);
                showSnackbar(resource.getMessage() != null
                        ? resource.getMessage()
                        : "Translation failed.");
                FileLogger.e(TAG, "Translation error: " + resource.getMessage());
            }
        });

        // Observe source language selection — only update button text
        // Re-translate is triggered directly from the language picker callback
        viewModel.getSourceLanguageLiveData().observe(getViewLifecycleOwner(), languageCode -> {
            if (languageCode != null) {
                getBinding().buttonSourceLanguage.setText(languageCode.toUpperCase());
            }
        });

       // Observe target language selection — only update button text
        // Re-translate is triggered directly from the language picker callback
        viewModel.getTargetLanguageLiveData().observe(getViewLifecycleOwner(), languageCode -> {
            if (languageCode != null) {
                getBinding().buttonTargetLanguage.setText(languageCode.toUpperCase());
            }
        });

        // Observe error messages from BaseViewModel
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
            }
        });

        // Observe favorite added one-shot event
        viewModel.getAddedToFavoriteLiveData().observe(getViewLifecycleOwner(), added -> {
            if (added != null && added) {
                showSnackbar("Added to favorites.");
                viewModel.clearAddedToFavorite();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Attaches click listeners for swap, copy, and favorite buttons.
     * Auto-translation is triggered via a debounced {@link TextWatcher}
     * instead of a dedicated translate button.</p>
     */
    @Override
    protected void initListeners() {
        // Swap languages button
        getBinding().buttonSwapLanguage.setOnClickListener(v -> {
            viewModel.swapLanguages();
            getBinding().editTextSourceInput.setText("");
            getBinding().textViewTranslationResult.setText("");
            currentTranslationResult = null;
            FileLogger.d(TAG, "Languages swapped by user.");
        });

        // Copy result button
        getBinding().buttonCopyResult.setOnClickListener(v -> {
            if (currentTranslationResult != null) {
                copyToClipboard(currentTranslationResult.getTranslatedText());
                showSnackbar("Translation copied to clipboard.");
            } else {
                showSnackbar("No translation to copy.");
            }
        });

        // Add to favorite button
        getBinding().buttonAddFavorite.setOnClickListener(v -> {
            if (currentTranslationResult != null) {
                viewModel.addToFavorite(currentTranslationResult);
            } else {
                showSnackbar("Translate something first.");
            }
        });

        // Source language selector button
        getBinding().buttonSourceLanguage.setOnClickListener(v -> {
            FileLogger.d(TAG, "Source language button clicked.");
            final String currentSource = viewModel.getSourceLanguageLiveData().getValue() != null
                    ? viewModel.getSourceLanguageLiveData().getValue() : "";
            final LanguagePickerBottomSheet sheet = LanguagePickerBottomSheet.newInstance(
                    LanguagePickerBottomSheet.MODE_SOURCE, currentSource);
            sheet.setOnLanguageSelectedListener((languageCode, mode) -> {
                viewModel.setSourceLanguage(languageCode);
                FileLogger.d(TAG, "Source language selected: " + languageCode);
                final String currentInput = getBinding().editTextSourceInput.getText().toString().trim();
                if (currentInput.length() >= MIN_AUTO_TRANSLATE_LENGTH) {
                    viewModel.translate(currentInput);
                }
            });
            sheet.show(getParentFragmentManager(), LanguagePickerBottomSheet.TAG);
        });

        // Target language selector button
        getBinding().buttonTargetLanguage.setOnClickListener(v -> {
            FileLogger.d(TAG, "Target language button clicked.");
            final String currentTarget = viewModel.getTargetLanguageLiveData().getValue() != null
                    ? viewModel.getTargetLanguageLiveData().getValue() : "";
            final LanguagePickerBottomSheet sheet = LanguagePickerBottomSheet.newInstance(
                    LanguagePickerBottomSheet.MODE_TARGET, currentTarget);
            sheet.setOnLanguageSelectedListener((languageCode, mode) -> {
                viewModel.setTargetLanguage(languageCode);
                FileLogger.d(TAG, "Target language selected: " + languageCode);
                final String currentInput = getBinding().editTextSourceInput.getText().toString().trim();
                if (currentInput.length() >= MIN_AUTO_TRANSLATE_LENGTH) {
                    viewModel.translate(currentInput);
                }
            });
            sheet.show(getParentFragmentManager(), LanguagePickerBottomSheet.TAG);
        });
		
		// Clear button — clears input and result
        getBinding().buttonTranslate.setOnClickListener(v -> {
            getBinding().editTextSourceInput.setText("");
            getBinding().textViewTranslationResult.setText("");
            currentTranslationResult = null;
            viewModel.clearTranslation();
            FileLogger.d(TAG, "Input and result cleared by user.");
        });

        // Auto-translate with debounce — fires 1 second after user stops typing
        getBinding().editTextSourceInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action required
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any pending auto-translate when user is still typing
                if (autoTranslateRunnable != null) {
                    autoTranslateHandler.removeCallbacks(autoTranslateRunnable);
                }
            }

            /**
             * Schedules auto-translation after the user stops typing.
             * Clears the result if input is empty.
             */
            @Override
            public void afterTextChanged(Editable s) {
                final String input = s.toString().trim();

                if (input.isEmpty()) {
                    // Clear result when input is cleared
                    getBinding().textViewTranslationResult.setText("");
                    currentTranslationResult = null;
                    return;
                }

                if (input.length() < MIN_AUTO_TRANSLATE_LENGTH) return;

                // Schedule auto-translate after debounce delay
                autoTranslateRunnable = () -> {
                    viewModel.detectLanguage(input);
                    viewModel.translate(input);
                    FileLogger.d(TAG, "Auto-translate triggered for: " + input);
                };
                autoTranslateHandler.postDelayed(autoTranslateRunnable, AUTO_TRANSLATE_DELAY_MS);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Lifecycle — cancel pending runnables on view destroy
    // -------------------------------------------------------------------------

    /**
     * Cancels any pending auto-translate runnable to prevent callbacks
     * from firing after the view has been destroyed.
     */
    @Override
    public void onDestroyView() {
        if (autoTranslateRunnable != null) {
            autoTranslateHandler.removeCallbacks(autoTranslateRunnable);
            autoTranslateRunnable = null;
        }
        super.onDestroyView();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Shows or hides the loading progress indicator and disables the
     * swap button during active translation to prevent conflicting requests.
     *
     * @param isLoading {@code true} to show loading; {@code false} to hide
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            shimmerStartTime = System.currentTimeMillis();
            getBinding().shimmerTranslate.setVisibility(View.VISIBLE);
            getBinding().shimmerTranslate.startShimmer();
            getBinding().textViewTranslationResult.setVisibility(View.INVISIBLE);
        } else {
            final long elapsed = System.currentTimeMillis() - shimmerStartTime;
            final long remaining = MIN_SHIMMER_DURATION_MS - elapsed;
            if (remaining > 0) {
                // Delay hiding shimmer so it's always visible for at least MIN_SHIMMER_DURATION_MS
                autoTranslateHandler.postDelayed(() -> {
                    getBinding().shimmerTranslate.stopShimmer();
                    getBinding().shimmerTranslate.setVisibility(View.GONE);
                    getBinding().textViewTranslationResult.setVisibility(View.VISIBLE);
                }, remaining);
            } else {
                getBinding().shimmerTranslate.stopShimmer();
                getBinding().shimmerTranslate.setVisibility(View.GONE);
                getBinding().textViewTranslationResult.setVisibility(View.VISIBLE);
            }
        }
        getBinding().buttonSwapLanguage.setEnabled(!isLoading);
    }

    /**
     * Copies the given text to the Android system clipboard.
     *
     * @param text the text to copy to clipboard; must not be null
     */
    private void copyToClipboard(@NonNull String text) {
        if (getContext() == null) return;

        final android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getContext()
                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE);

        if (clipboard != null) {
            final android.content.ClipData clip =
                    android.content.ClipData.newPlainText("translation", text);
            clipboard.setPrimaryClip(clip);
            FileLogger.d(TAG, "Text copied to clipboard.");
        }
    }
}

