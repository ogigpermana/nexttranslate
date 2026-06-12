package com.igoy86.nexttranslate.presentation.translate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.ArrayList;
import java.util.Locale;

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
import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.usecase.collection.AddWordToCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.GetCollectionsUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.NetworkUtils;
import com.igoy86.nexttranslate.util.UserSession;
import com.bumptech.glide.Glide;
import com.igoy86.nexttranslate.presentation.history.HistoryViewModel;
import com.igoy86.nexttranslate.presentation.history.HistoryViewModelFactory;
import com.igoy86.nexttranslate.domain.model.HistoryItem;
import java.util.List;

import com.igoy86.nexttranslate.util.UserSession;

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
 *
 * <p>UI state transitions:</p>
 * <ul>
 *     <li><b>Empty state</b>: history section visible, result panel hidden.</li>
 *     <li><b>Active state</b>: history hidden, result panel visible with
 *         shimmer during load and translated text on success.</li>
 * </ul>
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
	
	/** Android TextToSpeech engine — used for speaking translation results. */
    @Nullable
    private android.speech.tts.TextToSpeech tts;

    /** True once TTS engine has finished initializing. */
    private boolean isTtsReady = false;
	
	/** Launcher for the system SpeechRecognizer intent. */
    private ActivityResultLauncher<Intent> sttLauncher;

    /** Adapter for the horizontal history chip RecyclerView. Max 5 items. */
    private HistoryChipAdapter historyChipAdapter;

    /** ViewModel for observing history list to populate the chip row. */
    private HistoryViewModel historyViewModel;
	
	/** Use case for saving a word or phrase into a user collection. */
    private AddWordToCollectionUseCase addWordToCollectionUseCase;

    /** Use case for loading the list of available collections. */
    private GetCollectionsUseCase getCollectionsUseCase;

    /** Handler used to post delayed auto-translate and detect-language runnables. */
    private final Handler autoTranslateHandler = new Handler(Looper.getMainLooper());

    /** Runnable that triggers auto-translation after the debounce delay. */
    @Nullable
    private Runnable autoTranslateRunnable;
	
	/** Cached list of collections, updated once via observer in initObservers(). */
    @Nullable
    private List<CollectionItem> cachedCollections = null;

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

    /**
     * Flag to suppress auto-translate when text is being restored
     * from a history chip or history item tap.
     * Set to {@code true} before programmatic setText(), reset in afterTextChanged().
     */
    private boolean isRestoringFromHistory = false;

    // -------------------------------------------------------------------------
    // BaseFragment implementation
    // -------------------------------------------------------------------------
	
	
	@Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sttLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        final ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            final String spoken = matches.get(0);
                            getBinding().editTextSourceInput.setText(spoken);
                            getBinding().editTextSourceInput.setSelection(spoken.length());
                            FileLogger.d(TAG, "STT result: " + spoken);
                        }
                    }
                }
        );
    }

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
     *
     * <p>Also sets up the history RecyclerView as a horizontal list.
     * Adapter is set to null until history data is wired in a future iteration.</p>
     */
    @Override
    protected void initViews() {
        final AppContainer appContainer = NextTranslateApp.getContainer();
        final TranslateViewModelFactory factory = new TranslateViewModelFactory(
                appContainer.getTranslateTextUseCase(),
                appContainer.getRemoteTranslateTextUseCase(),
                appContainer.getDetectLanguageUseCase(),
                appContainer.getAddHistoryUseCase(),
                appContainer.getUpdateHistoryUseCase(),
                appContainer.getAddFavoriteUseCase()
        );

        viewModel = new ViewModelProvider(this, factory).get(TranslateViewModel.class);
        final AppContainer container = ((com.igoy86.nexttranslate.NextTranslateApp)
                requireActivity().getApplication()).getContainer();
        addWordToCollectionUseCase = container.getAddWordToCollectionUseCase();
        getCollectionsUseCase = container.getGetCollectionsUseCase();

        // Apply status bar inset to root LinearLayout (top padding only).
        // Keyboard inset applied to root so content shifts up when keyboard opens.
        ViewCompat.setOnApplyWindowInsetsListener(getBinding().getRoot(), (v, insets) -> {
            final int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            final int keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(0, top, 0, keyboard);
            return insets;
        });

        // Enable internal scrolling for the input EditText so it scrolls
        // within its own bounds (maxHeight) rather than pushing the outer layout.
        getBinding().editTextSourceInput.setMovementMethod(
                android.text.method.ScrollingMovementMethod.getInstance());

        // Enable internal scrolling for the result TextView so long translations
        // scroll within their maxHeight bounds — Yandex-style behaviour.
        getBinding().textViewTranslationResult.setMovementMethod(
                android.text.method.ScrollingMovementMethod.getInstance());

        // History RecyclerView — horizontal, max 5 chips
        getBinding().recyclerHistory.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(
                        getContext(),
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                        false
                )
        );

        // Setup HistoryChipAdapter — tap chip → load text ke input field
        historyChipAdapter = new HistoryChipAdapter(item -> {
            isRestoringFromHistory = true;
            viewModel.restoreFromHistory(item);
            getBinding().editTextSourceInput.setText(item.getSourceText());
            getBinding().editTextSourceInput.setSelection(item.getSourceText().length());
            // Show active state (hide history, show result panel)
            getBinding().layoutHistorySection.setVisibility(View.GONE);
            getBinding().dividerInputResult.setVisibility(View.VISIBLE);
            getBinding().buttonMic.setVisibility(View.GONE);
            getBinding().buttonClear.setVisibility(View.VISIBLE);
            FileLogger.d(TAG, "History chip restored: " + item.getSourceText());
        });
        getBinding().recyclerHistory.setAdapter(historyChipAdapter);

        // Setup HistoryViewModel untuk observe recent history
        final HistoryViewModelFactory historyFactory = new HistoryViewModelFactory(
                container.getGetAllHistoryUseCase(),
                container.getDeleteHistoryUseCase(),
                container.getClearAllHistoryUseCase(),
				container.getRestoreHistoryUseCase()
        );
        historyViewModel = new ViewModelProvider(this, historyFactory)
                .get(HistoryViewModel.class);

        // Icon history — kanan header, tap → HistoryFragment
        getBinding().btnHistoryIcon.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openHistoryFragment());

        // Wire TranslateFragment toolbar buttons
        getBinding().btnCollection.setOnClickListener(v ->
                ((MainActivity) requireActivity()).openCollectionFragment());

        getBinding().btnAvatar.setOnClickListener(v -> {
            final UserSession session = UserSession.getInstance(requireContext());
            if (session.isLoggedIn()) {
                startActivity(new android.content.Intent(getActivity(),
                        com.igoy86.nexttranslate.SettingsActivity.class));
            } else {
                startActivity(new android.content.Intent(getActivity(),
                        com.igoy86.nexttranslate.LoginActivity.class));
            }
        });
		
		// Initialize TTS engine
        tts = new TextToSpeech(requireContext(), status -> {
            isTtsReady = (status == TextToSpeech.SUCCESS);
            if (isTtsReady) {
                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        requireActivity().runOnUiThread(() ->
                                getBinding().buttonTtsResult.setIconResource(
                                        R.drawable.ic_stop));
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        requireActivity().runOnUiThread(() ->
                                getBinding().buttonTtsResult.setIconResource(
                                        R.drawable.ic_volume_up));
                    }

                    @Override
                    public void onError(String utteranceId) {
                        requireActivity().runOnUiThread(() ->
                                getBinding().buttonTtsResult.setIconResource(
                                        R.drawable.ic_volume_up));
                    }
                });
            }
            FileLogger.d(TAG, "TTS init status: " + status);
        });

        FileLogger.d(TAG, "TranslateFragment views initialized.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Observes translation result, loading state, language selections,
     * error messages, favorite events, and online mode from
     * {@link TranslateViewModel}.</p>
     */
    @Override
    protected void initObservers() {

        // --- Translation result ---
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

        // --- Source language label ---
        viewModel.getSourceLanguageLiveData().observe(getViewLifecycleOwner(), languageCode -> {
            if (languageCode != null) {
                getBinding().buttonSourceLanguage.setText(languageCode.toUpperCase());
            }
        });

        // --- Target language label ---
        viewModel.getTargetLanguageLiveData().observe(getViewLifecycleOwner(), languageCode -> {
            if (languageCode != null) {
                getBinding().buttonTargetLanguage.setText(languageCode.toUpperCase());
            }
        });

        // --- Favorite added event ---
        viewModel.getAddedToFavoriteLiveData().observe(getViewLifecycleOwner(), added -> {
            if (added != null && added) {
                showSnackbar("Added to favorites.");
                viewModel.clearAddedToFavorite();
            }
        });
		
		// --- Recent history chips (maks 5) ---
        // NOTE: Only update chip data here. Visibility of layoutHistorySection
        // is controlled exclusively by the EditText state (empty = show, active = hide).
        // Never override visibility here to avoid race conditions when the observer
        // re-emits after a translate mode switch or fragment resume.
        historyViewModel.getHistoryListLiveData().observe(getViewLifecycleOwner(),
                historyItems -> {
            if (historyChipAdapter != null) {
                historyChipAdapter.submitList(historyItems);
            }
            final boolean hasHistory = historyItems != null && !historyItems.isEmpty();
            // Only show history section if EditText is currently empty (idle state).
            // If user has active input, keep it hidden regardless of data changes.
            final boolean isInputEmpty = getBinding().editTextSourceInput
                    .getText().toString().trim().isEmpty();
            if (isInputEmpty) {
                getBinding().layoutHistorySection.setVisibility(
                        hasHistory ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            FileLogger.d(TAG, "History chips updated: " + (historyItems != null
                    ? historyItems.size() : 0) + " items, inputEmpty=" + isInputEmpty);
        });

        // --- Online / Offline mode — update chip label ---
        // Badge label removed from XML; mode is now reflected in chipTranslateMode text.
            viewModel.getIsOnlineModeLiveData().observe(getViewLifecycleOwner(), isOnline -> {
                if (isOnline == null) return;
                getBinding().chipTranslateMode.setText(isOnline
                        ? getString(R.string.mode_ai)
                        : getString(R.string.mode_fast));
                FileLogger.d(TAG, "Online mode observed: " + isOnline);
            });

            // -------------------------------------------------------------------------
            // Click listeners
            // -------------------------------------------------------------------------

            // --- Swap languages ---
            getBinding().buttonSwapLanguage.setOnClickListener(v -> {
                viewModel.swapLanguages();
                getBinding().editTextSourceInput.setText("");
                getBinding().textViewTranslationResult.setText("");
                currentTranslationResult = null;
                FileLogger.d(TAG, "Languages swapped by user.");
            });

            // --- Copy result ---
            getBinding().buttonCopyResult.setOnClickListener(v -> {
                if (currentTranslationResult != null) {
                    copyToClipboard(currentTranslationResult.getTranslatedText());
                    showSnackbar("Translation copied to clipboard.");
                } else {
                    showSnackbar("No translation to copy.");
                }
            });

            // --- Add to favorite ---
            getBinding().buttonAddFavorite.setOnClickListener(v -> {
                if (currentTranslationResult != null) {
                    viewModel.addToFavorite(currentTranslationResult);
                } else {
                    showSnackbar("Translate something first.");
                }
            });
		
		 // --- Save to collection ---
         getBinding().buttonSaveToCollection.setOnClickListener(v -> {
            if (currentTranslationResult == null) {
                showSnackbar("Translate something first.");
                return;
            }
            final String sourceText = currentTranslationResult.getSourceText();
            if (sourceText.length() > 100) {
                showSnackbar("Save feature is for words or short phrases only.");
                return;
            }
            showSaveToCollectionSheet(
                    sourceText,
                    currentTranslationResult.getTranslatedText()
            );
         });
	
		    // --- TTS result button ---
            getBinding().buttonTtsResult.setOnClickListener(v -> {
                if (!isTtsReady || tts == null) {
                    showSnackbar("TTS not ready.");
                    return;
                }
                // If already speaking — stop
                if (tts.isSpeaking()) {
                    tts.stop();
                    getBinding().buttonTtsResult.setIconResource(R.drawable.ic_volume_up);
                    FileLogger.d(TAG, "TTS stopped by user.");
                    return;
                }
                if (currentTranslationResult == null) {
                    showSnackbar("No translation to speak.");
                    return;
                }
                final String targetLang = viewModel.getTargetLanguageLiveData().getValue();
                final Locale locale = resolveLocale(targetLang);
                final int langResult = tts.setLanguage(locale);
                FileLogger.d(TAG, "TTS setLanguage result: " + langResult + " for locale: " + locale);
                if (langResult == TextToSpeech.LANG_MISSING_DATA
                        || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    showSnackbar("TTS language not available: " + locale.getDisplayLanguage());
                    FileLogger.w(TAG, "TTS lang not supported: " + locale);
                    // Offer to install voice data
                    final Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                    return;
                }
                // Extra check — engine claims available but may have no voice data
                final android.speech.tts.Voice voice = tts.getVoice();
                if (voice == null) {
                    showSnackbar("No TTS voice found for: " + locale.getDisplayLanguage());
                    FileLogger.w(TAG, "TTS voice null for locale: " + locale);
                    return;
                }
                final java.util.Set<String> features = voice.getFeatures();
                if (features != null && features.contains(
                        android.speech.tts.TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                    showSnackbar("TTS voice not installed for: " + locale.getDisplayLanguage()
                            + ". Installing...");
                    final Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                    return;
                }
                tts.speak(currentTranslationResult.getTranslatedText(),
                        TextToSpeech.QUEUE_FLUSH, null, "tts_result");
                FileLogger.d(TAG, "TTS speaking in locale: " + locale);
            });

            // --- Source language picker ---
            getBinding().buttonSourceLanguage.setOnClickListener(v -> {
                FileLogger.d(TAG, "Source language button clicked.");
                final String currentSource = viewModel.getSourceLanguageLiveData().getValue() != null
                        ? viewModel.getSourceLanguageLiveData().getValue() : "";
                final LanguagePickerBottomSheet sheet = LanguagePickerBottomSheet.newInstance(
                        LanguagePickerBottomSheet.MODE_SOURCE, currentSource);
                sheet.setOnLanguageSelectedListener((languageCode, mode) -> {
                    viewModel.setSourceLanguage(languageCode);
                    FileLogger.d(TAG, "Source language selected: " + languageCode);
                    final String currentInput = getBinding().editTextSourceInput.getText()
                            .toString().trim();
                    if (currentInput.length() >= MIN_AUTO_TRANSLATE_LENGTH) {
                        viewModel.translate(currentInput);
                    }
                });
                sheet.show(getParentFragmentManager(), LanguagePickerBottomSheet.TAG);
            });

            // --- Target language picker ---
            getBinding().buttonTargetLanguage.setOnClickListener(v -> {
                FileLogger.d(TAG, "Target language button clicked.");
                final String currentTarget = viewModel.getTargetLanguageLiveData().getValue() != null
                        ? viewModel.getTargetLanguageLiveData().getValue() : "";
                final LanguagePickerBottomSheet sheet = LanguagePickerBottomSheet.newInstance(
                        LanguagePickerBottomSheet.MODE_TARGET, currentTarget);
                sheet.setOnLanguageSelectedListener((languageCode, mode) -> {
                    viewModel.setTargetLanguage(languageCode);
                    FileLogger.d(TAG, "Target language selected: " + languageCode);
                    final String currentInput = getBinding().editTextSourceInput.getText()
                            .toString().trim();
                    if (currentInput.length() >= MIN_AUTO_TRANSLATE_LENGTH) {
                        viewModel.translate(currentInput);
                    }
                });
                sheet.show(getParentFragmentManager(), LanguagePickerBottomSheet.TAG);
            });

            // --- Clear button — resets to empty state ---
            getBinding().buttonClear.setOnClickListener(v -> {
                getBinding().editTextSourceInput.setText("");
                getBinding().textViewTranslationResult.setText("");
                currentTranslationResult = null;
                viewModel.clearTranslation();
                resetToEmptyState();
                FileLogger.d(TAG, "Input and result cleared by user.");
            });

            // --- Translate mode chip — opens TranslateModeBottomSheet ---
            // Reads current online-mode state from ViewModel and passes it to the
            // sheet so the switch reflects the active mode when the sheet opens.
            getBinding().chipTranslateMode.setOnClickListener(v -> {
                final boolean currentIsOnline = Boolean.TRUE.equals(
                        viewModel.getIsOnlineModeLiveData().getValue());
                final TranslateModeBottomSheet sheet =
                        TranslateModeBottomSheet.newInstance(currentIsOnline);
                sheet.setOnModeSelectedListener(isOnline -> {

                    // Offline mode guard — cegah switch ke Online jika offline mode aktif di Settings
                    if (isOnline && UserSession.getInstance(requireContext()).isOfflineMode()) {
                        showSnackbar("Offline mode is active. Disable it in Settings first.");
                        FileLogger.w(TAG, "Switch to online blocked — offline mode active in Settings.");
                        return;
                    }

                    // NetworkUtils guard — cegah switch ke Online jika tidak ada internet
                    if (isOnline && !NetworkUtils.isInternetAvailable(requireContext())) {
                        showSnackbar(getString(R.string.error_no_internet));
                        FileLogger.w(TAG, "Switch to online blocked — no internet.");
                        return;
                    }

                    viewModel.setOnlineMode(isOnline);
                    FileLogger.d(TAG, "Translate mode changed — online: " + isOnline);

                    final String currentInput = getBinding().editTextSourceInput
                            .getText().toString().trim();
                    if (currentInput.length() >= MIN_AUTO_TRANSLATE_LENGTH) {
                        // resetTranslatingGuard() — hanya reset isTranslating flag,
                        // tidak reset lastHistoryId agar sesi upsert tetap terjaga.
                        viewModel.resetTranslatingGuard();
                        viewModel.translate(currentInput);
                        FileLogger.d(TAG, "Re-translating after mode switch: " + currentInput);
                    }
                });
                sheet.show(getParentFragmentManager(), TranslateModeBottomSheet.TAG);
                FileLogger.d(TAG, "TranslateModeBottomSheet opened.");
            });

            // --- Camera button — switch to Photo tab ---
            getBinding().buttonCamera.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToPhotoTab();
                }
            });

            // --- Mic button — speech to text (TODO) ---
            getBinding().buttonMic.setOnClickListener(v -> {
                final String sourceLang = viewModel.getSourceLanguageLiveData().getValue();
                final Locale locale = resolveLocale(sourceLang);
                final Intent sttIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag());
                sttIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
			    sttIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
                sttIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
                sttIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L);
                sttLauncher.launch(sttIntent);
                FileLogger.d(TAG, "STT launched, locale: " + locale);
            });

            // --- History header — open HistoryFragment ---
            getBinding().buttonOpenHistory.setOnClickListener(v -> {
                ((MainActivity) requireActivity()).openHistoryFragment();
            });

            // --- Paste button — paste clipboard into input ---
            getBinding().buttonPaste.setOnClickListener(v -> {
                if (getContext() == null) return;
                final android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) getContext()
                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()
                        && clipboard.getPrimaryClip() != null
                        && clipboard.getPrimaryClip().getItemCount() > 0) {
                    final CharSequence text = clipboard.getPrimaryClip()
                            .getItemAt(0).coerceToText(getContext());
                    getBinding().editTextSourceInput.setText(text);
                    getBinding().editTextSourceInput.setSelection(text.length());
                    FileLogger.d(TAG, "Clipboard pasted into input.");
                }
            });

            // --- Auto-translate with debounce ---
            // Fires AUTO_TRANSLATE_DELAY_MS after user stops typing.
            getBinding().editTextSourceInput.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action required
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Cancel pending auto-translate while user is still typing
                    if (autoTranslateRunnable != null) {
                        autoTranslateHandler.removeCallbacks(autoTranslateRunnable);
                    }
                }

                /**
                 * Switches UI to empty or active state based on input length,
                 * then schedules auto-translation after the debounce delay.
                 */
                @Override
                public void afterTextChanged(Editable s) {
                    final String input = s.toString().trim();

                    if (input.isEmpty()) {
                        getBinding().textViewTranslationResult.setText("");
                        currentTranslationResult = null;
                        resetToEmptyState();
                        isRestoringFromHistory = false;
                        return;
                    }

                    // Switch to active state — show result panel, hide history
                    getBinding().layoutHistorySection.setVisibility(View.GONE);
                    getBinding().dividerInputResult.setVisibility(View.VISIBLE);
                    getBinding().buttonMic.setVisibility(View.GONE);
                    getBinding().buttonClear.setVisibility(View.VISIBLE);

                    // Skip auto-translate if text was restored from history chip/item
                    if (isRestoringFromHistory) {
                        isRestoringFromHistory = false;
                        FileLogger.d(TAG, "Skipping auto-translate — restored from history.");
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
			
		getCollectionsUseCase.execute().observe(getViewLifecycleOwner(), collections -> {
            cachedCollections = collections;
        }); 
     }

        /** {@inheritDoc} Listeners are wired in {@link #initObservers()} for this fragment. */
        @Override
        protected void initListeners() {
            // No-op — all listeners wired in initObservers()
        }
	
     private void showSaveToCollectionSheet(@NonNull String word, @NonNull String definition) {
        if (cachedCollections == null || cachedCollections.isEmpty()) {
            showSnackbar("Create a collection first from the Collections tab.");
            return;
        }

        final BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        final android.view.View sheetView = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_save_to_collection, null);
        sheet.setContentView(sheetView);

        final androidx.recyclerview.widget.RecyclerView rv =
                sheetView.findViewById(R.id.recyclerSaveToCollection);
        final com.google.android.material.button.MaterialButton btnSave =
                sheetView.findViewById(R.id.btnConfirmSave);

        rv.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));

        final long[] selectedId = {cachedCollections.get(0).getId()};
        final int[] selectedPos = {0};

        rv.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<
                androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull android.view.ViewGroup parent, int viewType) {
                android.view.View itemView = getLayoutInflater().inflate(
                        R.layout.item_save_collection_option, parent, false);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(
                    @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder,
                    int position) {
                final CollectionItem item = cachedCollections.get(position);
                final android.widget.TextView tvName =
                        holder.itemView.findViewById(R.id.textCollectionName);
                final android.widget.RadioButton radio =
                        holder.itemView.findViewById(R.id.radioSelected);
                final android.view.View colorDot =
                        holder.itemView.findViewById(R.id.viewColorIndicator);

                tvName.setText(item.getName());
                radio.setChecked(position == selectedPos[0]);

                try {
                    colorDot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor(item.getColorHex())
                            )
                    );
                } catch (Exception ignored) {}

                holder.itemView.setOnClickListener(v -> {
                    selectedPos[0] = holder.getAdapterPosition();
                    selectedId[0] = cachedCollections.get(selectedPos[0]).getId();
                    notifyDataSetChanged();
                });
            }

            @Override
            public int getItemCount() {
                return cachedCollections.size();
            }
        });

        btnSave.setOnClickListener(v -> {
            addWordToCollectionUseCase.execute(
                    selectedId[0], word, definition,
                    () -> {
                        showSnackbar("Saved to collection.");
                        sheet.dismiss();
                    }
            );
        });

        sheet.show();
    }
 

    // -------------------------------------------------------------------------
    // Lifecycle
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
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAvatarIfLoggedIn();
        // Sync offline mode from UserSession → ViewModel
        final boolean isOffline = UserSession.getInstance(requireContext()).isOfflineMode();
        viewModel.setOnlineMode(!isOffline);
        FileLogger.d(TAG, "onResume: offline mode = " + isOffline);
    }

    /**
     * Loads the user's Google profile photo into the avatar button via Glide.
     * Falls back to ic_person if not logged in or no photo URL.
     */
    private void loadAvatarIfLoggedIn() {
        if (getContext() == null) return;
        final UserSession session = UserSession.getInstance(requireContext());
        final String photoUrl = session.getPhotoUrl();
        if (session.isLoggedIn() && photoUrl != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(getBinding().btnAvatar);
        } else {
            getBinding().btnAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    
	/**
     * Called by {@link com.igoy86.nexttranslate.presentation.history.HistoryFragment}
     * when the user taps a history item and pops back to this screen.
     *
     * @param item the history item to restore
     */
    public void restoreFromHistory(@NonNull HistoryItem item) {
        isRestoringFromHistory = true;
        viewModel.restoreFromHistory(item);
        getBinding().editTextSourceInput.setText(item.getSourceText());
        getBinding().editTextSourceInput.setSelection(item.getSourceText().length());
        getBinding().layoutHistorySection.setVisibility(View.GONE);
        getBinding().dividerInputResult.setVisibility(View.VISIBLE);
        getBinding().buttonMic.setVisibility(View.GONE);
        getBinding().buttonClear.setVisibility(View.VISIBLE);
        FileLogger.d(TAG, "restoreFromHistory called from HistoryFragment: " + item.getSourceText());
    }
	
	/**
     * Called by {@link com.igoy86.nexttranslate.presentation.photo.PhotoFragment}
     * when the user taps "Terjemahkan" after OCR extraction.
     *
     * <p>Pre-fills the source input with the OCR-extracted text and
     * triggers auto-translation immediately.</p>
     *
     * @param ocrText the text extracted via ML Kit OCR; must not be null
     */
    public void restoreFromOcr(@NonNull String ocrText) {
        if (ocrText.isEmpty()) return;
        isRestoringFromHistory = false;
        getBinding().editTextSourceInput.setText(ocrText);
        getBinding().editTextSourceInput.setSelection(ocrText.length());
        getBinding().layoutHistorySection.setVisibility(View.GONE);
        getBinding().dividerInputResult.setVisibility(View.VISIBLE);
        getBinding().buttonMic.setVisibility(View.GONE);
        getBinding().buttonClear.setVisibility(View.VISIBLE);
        viewModel.detectLanguage(ocrText);
        viewModel.translate(ocrText);
        FileLogger.d(TAG, "restoreFromOcr called: " + ocrText);
    }
	
	/**
     * Resolves a language code string (e.g. "id", "en", "ja") to a {@link Locale}.
     * Falls back to the device default locale if the code is null or unrecognized.
     *
     * @param langCode ISO 639-1 language code, or null
     * @return the resolved Locale
     */
    @NonNull
    private Locale resolveLocale(@Nullable String langCode) {
        if (langCode == null || langCode.isEmpty()) return Locale.getDefault();
        switch (langCode.toLowerCase(Locale.ROOT)) {
            case "id": return new Locale("in"); // Indonesian
            case "en": return Locale.ENGLISH;
            case "ja": return Locale.JAPANESE;
            case "zh": return Locale.CHINESE;
            case "ko": return Locale.KOREAN;
            case "fr": return Locale.FRENCH;
            case "de": return Locale.GERMAN;
            case "it": return Locale.ITALIAN;
            default:   return new Locale(langCode);
        }
    }
	
    /**
     * Resets the UI to the empty/idle state:
     * <ul>
     *     <li>Hides result panel (divider, shimmer, result text, result actions)</li>
     *     <li>Shows history section</li>
     *     <li>Restores mic button, hides clear button</li>
     * </ul>
     */
    private void resetToEmptyState() {
        getBinding().dividerInputResult.setVisibility(View.GONE);
        getBinding().shimmerTranslate.setVisibility(View.GONE);
        getBinding().textViewTranslationResult.setVisibility(View.GONE);
        getBinding().layoutResultActions.setVisibility(View.GONE);
        getBinding().layoutHistorySection.setVisibility(View.VISIBLE);
        getBinding().buttonClear.setVisibility(View.GONE);
        getBinding().buttonMic.setVisibility(View.VISIBLE);
    }

    /**
     * Shows or hides the shimmer loading indicator inside the result panel.
     *
     * <p>When loading starts, the divider and shimmer become visible while
     * the result text and action bar are hidden. A minimum shimmer duration
     * ({@link #MIN_SHIMMER_DURATION_MS}) is enforced so the animation is
     * always noticeable even for very fast translations.</p>
     *
     * <p>The swap button is disabled during active translation to prevent
     * conflicting requests.</p>
     *
     * @param isLoading {@code true} to show shimmer; {@code false} to reveal result
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            shimmerStartTime = System.currentTimeMillis();
            getBinding().dividerInputResult.setVisibility(View.VISIBLE);
            getBinding().shimmerTranslate.setVisibility(View.VISIBLE);
            getBinding().shimmerTranslate.startShimmer();
            getBinding().textViewTranslationResult.setVisibility(View.GONE);
            getBinding().layoutResultActions.setVisibility(View.GONE);
        } else {
            final long elapsed = System.currentTimeMillis() - shimmerStartTime;
            final long remaining = MIN_SHIMMER_DURATION_MS - elapsed;
            if (remaining > 0) {
                autoTranslateHandler.postDelayed(() -> {
                    if (getBinding() == null) return;
                    getBinding().shimmerTranslate.stopShimmer();
                    getBinding().shimmerTranslate.setVisibility(View.GONE);
                    getBinding().textViewTranslationResult.setVisibility(View.VISIBLE);
                    getBinding().layoutResultActions.setVisibility(View.VISIBLE);
                }, remaining);
            } else {
                getBinding().shimmerTranslate.stopShimmer();
                getBinding().shimmerTranslate.setVisibility(View.GONE);
                getBinding().textViewTranslationResult.setVisibility(View.VISIBLE);
                getBinding().layoutResultActions.setVisibility(View.VISIBLE);
            }
        }
        getBinding().buttonSwapLanguage.setEnabled(!isLoading);
    }

    /**
     * Copies the given text to the Android system clipboard.
     *
     * @param text the text to copy; must not be null
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

