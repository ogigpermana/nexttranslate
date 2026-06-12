package com.igoy86.nexttranslate.presentation.dialog;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.R;
import com.igoy86.nexttranslate.databinding.FragmentDialogModeBinding;
import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.domain.usecase.translate.RemoteTranslateTextUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.TranslateTextUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.NetworkUtils;
import com.igoy86.nexttranslate.util.Resource;
import com.igoy86.nexttranslate.util.UserSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DialogModeFragment — Two-way interpreter screen (Yandex-style).
 *
 * <p>User A (left mic) and User B (right mic) each tap their mic to speak.
 * Speech is recognized via STT, translated via Groq AI (if online) or
 * ML Kit (if offline/no internet), shown in the conversation log, and
 * spoken aloud via TTS to the other person.</p>
 */
public class DialogModeFragment extends BaseFragment<FragmentDialogModeBinding> {

    private static final String TAG = "DialogModeFragment";

    private static final int SPEAKER_A = 0;
    private static final int SPEAKER_B = 1;

    // Languages
    private String langA = "id";
    private String langB = "en";

    // Use cases
    private RemoteTranslateTextUseCase remoteTranslateTextUseCase;
    private TranslateTextUseCase translateTextUseCase;

    // TTS
    @Nullable private TextToSpeech tts;
    private boolean isTtsReady = false;

    // Conversation log
    private final List<ConversationItem> conversationItems = new ArrayList<>();
    private ConversationAdapter conversationAdapter;

    // STT launchers
    private ActivityResultLauncher<Intent> sttLauncherA;
    private ActivityResultLauncher<Intent> sttLauncherB;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate(@Nullable android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init use cases from AppContainer
        remoteTranslateTextUseCase = NextTranslateApp.getContainer().getRemoteTranslateTextUseCase();
        translateTextUseCase = NextTranslateApp.getContainer().getTranslateTextUseCase();

        // STT launcher User A
        sttLauncherA = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        final ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            handleSpeechResult(SPEAKER_A, matches.get(0));
                        }
                    }
                    resetMicState(SPEAKER_A);
                });

        // STT launcher User B
        sttLauncherB = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        final ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            handleSpeechResult(SPEAKER_B, matches.get(0));
                        }
                    }
                    resetMicState(SPEAKER_B);
                });
    }

    // -------------------------------------------------------------------------
    // BaseFragment contract
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    protected FragmentDialogModeBinding initBinding(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable android.view.ViewGroup container) {
        return FragmentDialogModeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initViews() {
        // Status bar inset
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                getBinding().rootDialog, (v, insets) -> {
                    final int top = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                    v.setPadding(0, top, 0, 0);
                    return insets;
                });

        // Conversation RecyclerView
        conversationAdapter = new ConversationAdapter();
        final LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        llm.setStackFromEnd(true);
        getBinding().recyclerConversation.setLayoutManager(llm);
        getBinding().recyclerConversation.setAdapter(conversationAdapter);

        // Init TTS
        tts = new TextToSpeech(requireContext(), status -> {
            isTtsReady = (status == TextToSpeech.SUCCESS);
            FileLogger.d(TAG, "TTS init: " + status);
        });

        updateLangButtons();
        getBinding().layoutEmptyDialog.setVisibility(
            conversationItems.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
		
		// Setup toolbar menu
        getBinding().toolbarDialog.inflateMenu(R.menu.menu_dialog);
        getBinding().toolbarDialog.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_dialog) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Clear conversation")
                        .setMessage("Are you sure you want to clear this conversation?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            conversationItems.clear();
                            conversationAdapter.submitList(new ArrayList<>());
                            getBinding().layoutEmptyDialog.setVisibility(android.view.View.VISIBLE);
                            FileLogger.d(TAG, "Conversation cleared.");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void initObservers() {}

    @Override
    protected void initListeners() {

        // Mic User A
        getBinding().fabMicA.setOnClickListener(v -> launchStt(SPEAKER_A));

        // Mic User B
        getBinding().fabMicB.setOnClickListener(v -> launchStt(SPEAKER_B));

        // Lang selector User A
        getBinding().btnLangA.setOnClickListener(v -> showLanguagePicker(SPEAKER_A));

        // Lang selector User B
        getBinding().btnLangB.setOnClickListener(v -> showLanguagePicker(SPEAKER_B));
    }

    // -------------------------------------------------------------------------
    // STT
    // -------------------------------------------------------------------------

    private void launchStt(int speaker) {
        final String lang = (speaker == SPEAKER_A) ? langA : langB;
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolveLocale(lang).toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L);

        // Visual feedback — dim the tapped mic
        if (speaker == SPEAKER_A) {
            getBinding().fabMicA.setAlpha(0.5f);
        } else {
            getBinding().fabMicB.setAlpha(0.5f);
        }

        FileLogger.d(TAG, "STT launched for speaker " + speaker + ", lang: " + lang);
        if (speaker == SPEAKER_A) {
            sttLauncherA.launch(intent);
        } else {
            sttLauncherB.launch(intent);
        }
    }

    // -------------------------------------------------------------------------
    // Translation
    // -------------------------------------------------------------------------

    /**
     * Handles STT result — translates using AI (online) or ML Kit (offline).
     */
    private void handleSpeechResult(int speaker, @NonNull String spokenText) {
        FileLogger.d(TAG, "Speech result speaker " + speaker + ": " + spokenText);

        final String sourceLang = (speaker == SPEAKER_A) ? langA : langB;
        final String targetLang = (speaker == SPEAKER_A) ? langB : langA;

        final boolean isOfflineMode = UserSession.getInstance(requireContext()).isOfflineMode();
        final boolean hasInternet   = NetworkUtils.isInternetAvailable(requireContext());
        final boolean useAi         = !isOfflineMode && hasInternet;

        FileLogger.d(TAG, "Translation engine: " + (useAi ? "Groq AI" : "ML Kit"));

        if (useAi) {
            translateWithAi(speaker, spokenText, sourceLang, targetLang);
        } else {
            translateWithMlKit(speaker, spokenText, sourceLang, targetLang);
        }
    }

    /**
     * Translates via Groq AI (remote) — natural conversation quality.
     */
    private void translateWithAi(int speaker, @NonNull String text,
                                  @NonNull String sourceLang, @NonNull String targetLang) {
        remoteTranslateTextUseCase.execute(text, getLangLabel(sourceLang), getLangLabel(targetLang))
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.isSuccess() && resource.getData() != null) {
                        final String translated = resource.getData().getTranslatedText();
                        addToConversation(speaker, text, translated, targetLang);
                    } else if (resource.isError()) {
                        FileLogger.w(TAG, "AI translate failed, falling back to ML Kit: "
                                + resource.getMessage());
                        translateWithMlKit(speaker, text, sourceLang, targetLang);
                    }
                });
    }

    /**
     * Translates via ML Kit — offline fallback.
     */
    private void translateWithMlKit(int speaker, @NonNull String text,
                                     @NonNull String sourceLang, @NonNull String targetLang) {
        translateTextUseCase.execute(text, sourceLang, targetLang)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.isSuccess() && resource.getData() != null) {
                        final String translated = resource.getData().getTranslatedText();
                        addToConversation(speaker, text, translated, targetLang);
                    } else if (resource.isError()) {
                        FileLogger.w(TAG, "ML Kit translate failed: " + resource.getMessage());
                        showSnackbar(getString(R.string.error_translation_failed));
                    }
                });
    }

    /**
     * Adds a conversation bubble and speaks the translation aloud.
     */
    private void addToConversation(int speaker, @NonNull String original,
                                    @NonNull String translated, @NonNull String targetLang) {
        final ConversationItem item = new ConversationItem(
                speaker == SPEAKER_A, original, translated);
        conversationItems.add(item);
        conversationAdapter.submitList(new ArrayList<>(conversationItems));
        getBinding().recyclerConversation.smoothScrollToPosition(conversationItems.size() - 1);
        getBinding().layoutEmptyDialog.setVisibility(android.view.View.GONE);
        speakText(translated, targetLang);
    }

    // -------------------------------------------------------------------------
    // TTS
    // -------------------------------------------------------------------------

    private void speakText(@NonNull String text, @NonNull String langCode) {
        if (!isTtsReady || tts == null) return;
        final Locale locale = resolveLocale(langCode);
        final int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            FileLogger.w(TAG, "TTS lang not supported: " + locale);
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dialog_tts");
    }

    // -------------------------------------------------------------------------
    // Language picker
    // -------------------------------------------------------------------------

    private void showLanguagePicker(int speaker) {
        final String[] langs  = {"id", "en", "ja", "zh", "ko", "fr", "de", "it", "ar", "es"};
        final String[] labels = {
                "Indonesian", "English", "Japanese", "Chinese",
                "Korean", "French", "German", "Italian", "Arabic", "Spanish"
        };
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setItems(labels, (dialog, which) -> {
                    if (speaker == SPEAKER_A) langA = langs[which];
                    else                      langB = langs[which];
                    updateLangButtons();
                    FileLogger.d(TAG, "Speaker " + speaker + " lang: " + langs[which]);
                })
                .show();
    }

    private void updateLangButtons() {
        getBinding().btnLangA.setText(getLangLabel(langA));
        getBinding().btnLangB.setText(getLangLabel(langB));
    }

    @NonNull
    private String getLangLabel(@NonNull String code) {
        switch (code.toLowerCase(Locale.ROOT)) {
            case "id": return "Indonesian";
            case "en": return "English";
            case "ja": return "Japanese";
            case "zh": return "Chinese";
            case "ko": return "Korean";
            case "fr": return "French";
            case "de": return "German";
            case "it": return "Italian";
            case "ar": return "Arabic";
            case "es": return "Spanish";
            default:   return code.toUpperCase(Locale.ROOT);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @NonNull
    private Locale resolveLocale(@NonNull String langCode) {
        switch (langCode.toLowerCase(Locale.ROOT)) {
            case "id": return new Locale("in");
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

    private void resetMicState(int speaker) {
        if (getView() == null) return;
        if (speaker == SPEAKER_A) {
            getBinding().fabMicA.setAlpha(1.0f);
        } else {
            getBinding().fabMicB.setAlpha(1.0f);
        }
    }

    @Override
    public void onDestroyView() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroyView();
    }
}
