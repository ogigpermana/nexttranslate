package com.igoy86.nexttranslate.presentation.translate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.domain.usecase.favorite.AddFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.AddHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.UpdateHistoryUseCase;
import com.igoy86.nexttranslate.domain.repository.HistoryRepository;
import com.igoy86.nexttranslate.domain.usecase.translate.DetectLanguageUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.TranslateTextUseCase;
import com.igoy86.nexttranslate.domain.usecase.translate.RemoteTranslateTextUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

/**
 * ViewModel for the main translation screen.
 *
 * <p>Manages the UI state for text translation, language detection,
 * saving to history, and adding to favorites. Survives configuration
 * changes (e.g. screen rotation) and exposes state via {@link LiveData}.</p>
 *
 * <p>Depends on:</p>
 * <ul>
 *     <li>{@link TranslateTextUseCase} — performs the ML Kit translation</li>
 *     <li>{@link DetectLanguageUseCase} — detects the source language</li>
 *     <li>{@link AddHistoryUseCase} — persists each translation to history</li>
 *     <li>{@link AddFavoriteUseCase} — bookmarks a translation as favorite</li>
 * </ul>
 *
 * <p>Instantiated via {@link TranslateViewModelFactory}.</p>
 */
public class TranslateViewModel extends BaseViewModel {

    /** Tag used for logging events originating from this ViewModel. */
    private static final String TAG = "TranslateViewModel";

    // -------------------------------------------------------------------------
    // Use Cases
    // -------------------------------------------------------------------------

    /** Use case for performing ML Kit text translation. */
    @NonNull
    private final TranslateTextUseCase translateTextUseCase;
	
	/** Use case for performing remote Groq text translation (online). */
    @NonNull
    private final RemoteTranslateTextUseCase remoteTranslateTextUseCase;

    /** Use case for detecting the language of the input text. */
    @NonNull
    private final DetectLanguageUseCase detectLanguageUseCase;

    /** Use case for persisting a completed translation to history. */
    @NonNull
    private final AddHistoryUseCase addHistoryUseCase;

    /** Use case for bookmarking a translation as a favorite. */
    @NonNull
    private final AddFavoriteUseCase addFavoriteUseCase;
	
	/**
     * Use case for updating an existing history entry in-place.
     * Used on the second and subsequent translates within the same session.
     */
    @NonNull
    private final UpdateHistoryUseCase updateHistoryUseCase;

    // -------------------------------------------------------------------------
    // UI State LiveData
    // -------------------------------------------------------------------------

    /**
     * The current translation result.
     * Emits {@link Resource#loading(Object)} during translation,
     * {@link Resource#success(Object)} on completion, or
     * {@link Resource#error(String, Object)} on failure.
     */
    private final MutableLiveData<Resource<TranslationResult>> translationResultLiveData =
            new MutableLiveData<>();

    /**
     * The detected source language BCP-47 code.
     * Emits {@link Resource#success(Object)} with the detected code (e.g. "en"),
     * or {@link Resource#error(String, Object)} if detection fails.
     */
    private final MutableLiveData<Resource<String>> detectedLanguageLiveData =
            new MutableLiveData<>();

    /**
     * The currently selected source language BCP-47 code.
     * Defaults to English ("en").
     */
    private final MutableLiveData<String> sourceLanguageLiveData =
            new MutableLiveData<>("en");

    /**
     * The currently selected target language BCP-47 code.
     * Defaults to Indonesian ("id").
     */
    private final MutableLiveData<String> targetLanguageLiveData =
            new MutableLiveData<>("id");

    /**
     * Emits {@code true} when a translation has been successfully
     * added to favorites, triggering a one-shot UI event.
     */
    private final MutableLiveData<Boolean> addedToFavoriteLiveData =
            new MutableLiveData<>();

    /**
     * Guard flag to prevent duplicate translate() calls triggered by
     * detectLanguage() auto-updating sourceLanguageLiveData.
     * Set to {@code true} while a translation is already in progress.
     */
    private boolean isTranslating = false;

    /**
     * Guard flag to distinguish user-initiated language changes (from picker)
     * vs. auto-detected language updates (from detectLanguage()).
     * {@code true} means the next sourceLanguage update came from auto-detect,
     * so the observer in the Fragment should NOT trigger a re-translate.
     */
    private boolean isAutoDetectUpdate = false;

    /**
     * The source text of the most recently saved history entry.
     *
     * <p>Used together with {@link #lastSavedTargetLang} to prevent
     * duplicate history entries when the same text is re-translated
     * (e.g. after switching online/offline mode or fragment recreation).</p>
     *
     * <p>{@code null} means nothing has been saved in this ViewModel session.</p>
     */
    @Nullable
    private String lastSavedSourceText = null;

    /**
     * The target language code of the most recently saved history entry.
     *
     * <p>Used together with {@link #lastSavedSourceText} to form a
     * composite key for deduplication. A language change with the same
     * source text is treated as a new unique entry and will be saved.</p>
     */
    @Nullable
    private String lastSavedTargetLang = null;
	
	/**
     * The database ID of the history entry created in the current translate session.
     *
     * <p>On the first successful translation in a session, a new row is INSERTed
     * and its auto-generated ID is stored here. Subsequent translations in the
     * same session call UPDATE on this ID instead of creating a new row —
     * replicating Google Translate's "one entry per session" behaviour.</p>
     *
     * <p>{@code -1L} means no entry has been saved in the current session.
     * Reset to {@code -1L} by {@link #resetSession()} when the user clears
     * the input or changes the target language.</p>
     */
    private long lastHistoryId = -1L;

    /**
     * The target language code active at the time the current session started.
     *
     * <p>Used to detect when the user changes the target language mid-session.
     * A language change resets the session ({@link #resetSession()}) so the
     * next translation is INSERTed as a fresh entry rather than UPDATE-ing
     * the previous language pair's entry.</p>
     *
     * <p>{@code null} means no session is active.</p>
     */
    @Nullable
    private String lastSessionTargetLang = null;

	
	/** Whether online mode (Groq) is active. Defaults to false (offline/ML Kit). */
    private final MutableLiveData<Boolean> isOnlineModeLiveData = new MutableLiveData<>(false);

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link TranslateViewModel} with the required use cases.
     *
     * @param translateTextUseCase  use case for text translation
     * @param detectLanguageUseCase use case for language detection
     * @param addHistoryUseCase     use case for saving to history
     * @param addFavoriteUseCase    use case for adding to favorites
     */
     public TranslateViewModel(
          @NonNull TranslateTextUseCase translateTextUseCase,
          @NonNull RemoteTranslateTextUseCase remoteTranslateTextUseCase,
          @NonNull DetectLanguageUseCase detectLanguageUseCase,
          @NonNull AddHistoryUseCase addHistoryUseCase,
          @NonNull UpdateHistoryUseCase updateHistoryUseCase,
          @NonNull AddFavoriteUseCase addFavoriteUseCase
     ) {
          this.translateTextUseCase = translateTextUseCase;
          this.remoteTranslateTextUseCase = remoteTranslateTextUseCase;
          this.detectLanguageUseCase = detectLanguageUseCase;
          this.addHistoryUseCase = addHistoryUseCase;
          this.updateHistoryUseCase = updateHistoryUseCase;
          this.addFavoriteUseCase = addFavoriteUseCase;
     }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Initiates a translation of the given source text using the currently
     * selected source and target language codes.
     *
     * <p>Emits loading, success, or error states via
     * {@link #getTranslationResultLiveData()}. On success, the result is
     * automatically persisted to translation history.</p>
     *
     * @param sourceText the text to translate; must not be null or empty
     */
    public void translate(@NonNull String sourceText) {
        final String sourceLang = sourceLanguageLiveData.getValue();
        final String targetLang = targetLanguageLiveData.getValue();

        if (sourceLang == null || targetLang == null) {
            setError("Source or target language is not selected.");
            return;
        }

        if (sourceText.trim().isEmpty()) {
            FileLogger.w(TAG, "Translate called with empty source text.");
            return;
        }

        FileLogger.d(TAG, "Translating: [" + sourceLang + " → " + targetLang + "] " + sourceText);

        // Guard — prevent duplicate calls while already translating
        if (isTranslating) {
            FileLogger.w(TAG, "Translate already in progress, skipping duplicate call.");
            return;
        }
        isTranslating = true;

        // Emit loading immediately so shimmer shows before MediatorLiveData activates
        translationResultLiveData.setValue(Resource.loading(null));

        // Use MediatorLiveData to safely observe use case result
        // without leaking an unremovable observer via observeForever.
        final MediatorLiveData<Resource<TranslationResult>> mediator = new MediatorLiveData<>();
        final boolean isOnline = Boolean.TRUE.equals(isOnlineModeLiveData.getValue());
        FileLogger.d(TAG, "Translation mode: " + (isOnline ? "Online (Groq)" : "Offline (ML Kit)"));
        final LiveData<Resource<TranslationResult>> source = isOnline
                ? remoteTranslateTextUseCase.execute(sourceText, sourceLang, targetLang)
                : translateTextUseCase.execute(sourceText, sourceLang, targetLang);

        mediator.addSource(source, resource -> {
            translationResultLiveData.setValue(resource);

            // Auto-save to history on successful translation
            if (resource.isSuccess() && resource.getData() != null) {
                saveToHistory(resource.getData());
            }

            // Remove source once terminal state reached to avoid leaking
            if (resource.isSuccess() || resource.isError()) {
                isTranslating = false;
                mediator.removeSource(source);
            }
        });

        // Observe mediator with no-op observer to activate the chain
        mediator.observeForever(new Observer<Resource<TranslationResult>>() {
            @Override
            public void onChanged(Resource<TranslationResult> ignored) {
                // Forward already handled inside addSource above
            }
        });
    }

    /**
     * Detects the language of the given text using ML Kit Language Identification.
     *
     * <p>Emits the detected BCP-47 language code via
     * {@link #getDetectedLanguageLiveData()} on success.</p>
     *
     * @param text the text whose language is to be detected; must not be null
     */
    public void detectLanguage(@NonNull String text) {
        if (text.trim().isEmpty()) {
            FileLogger.w(TAG, "DetectLanguage called with empty text.");
            return;
        }

        // Use MediatorLiveData to safely observe use case result
        // without leaking an unremovable observer via observeForever.
        final MediatorLiveData<Resource<String>> mediator = new MediatorLiveData<>();
        final LiveData<Resource<String>> source = detectLanguageUseCase.execute(text);

        mediator.addSource(source, resource -> {
            detectedLanguageLiveData.setValue(resource);

            // Auto-update source language on successful detection
            if (resource.isSuccess() && resource.getData() != null) {
                isAutoDetectUpdate = true;
                sourceLanguageLiveData.setValue(resource.getData());
                FileLogger.d(TAG, "Source language auto-updated to: " + resource.getData());
            }

            // Remove source once terminal state reached to avoid leaking
            if (resource.isSuccess() || resource.isError()) {
                mediator.removeSource(source);
            }
        });

        mediator.observeForever(new Observer<Resource<String>>() {
            @Override
            public void onChanged(Resource<String> ignored) {
                // Forward already handled inside addSource above
            }
        });
    }

    /**
     * Returns whether the last sourceLanguage update was triggered by
     * auto-detection (not by the user picking from the picker).
     * The Fragment must call {@link #consumeAutoDetectUpdate()} after reading this.
     *
     * @return {@code true} if the update was from auto-detect
     */
    public boolean isAutoDetectUpdate() {
        return isAutoDetectUpdate;
    }

    /**
     * Resets the auto-detect update flag after the Fragment has consumed it.
     */
    public void consumeAutoDetectUpdate() {
        isAutoDetectUpdate = false;
    }

    /**
     * Updates the selected source language.
     *
     * @param languageCode the BCP-47 code of the new source language (e.g. "en")
     */
    public void setSourceLanguage(@NonNull String languageCode) {
        sourceLanguageLiveData.setValue(languageCode);
        FileLogger.d(TAG, "Source language set to: " + languageCode);
    }

    /**
     * Updates the selected target language.
     *
     * @param languageCode the BCP-47 code of the new target language (e.g. "id")
     */
    public void setTargetLanguage(@NonNull String languageCode) {
        targetLanguageLiveData.setValue(languageCode);
        FileLogger.d(TAG, "Target language set to: " + languageCode);
    }

    /**
     * Swaps the current source and target language codes.
     *
     * <p>Also clears the current translation result since the language
     * pair has changed.</p>
     */
    public void swapLanguages() {
        final String currentSource = sourceLanguageLiveData.getValue();
        final String currentTarget = targetLanguageLiveData.getValue();

        if (currentSource != null && currentTarget != null) {
            sourceLanguageLiveData.setValue(currentTarget);
            targetLanguageLiveData.setValue(currentSource);
            translationResultLiveData.setValue(null);
            FileLogger.d(TAG, "Languages swapped: "
                    + currentTarget + " → " + currentSource);
        }
    }
	
	/**
     * Clears the current translation result and resets the translate session.
     *
     * <p>Resetting the session means the next translation will INSERT a fresh
     * history entry instead of updating the previous one. This is the correct
     * behaviour when the user explicitly clears the input field.</p>
     */
    public void clearTranslation() {
        translationResultLiveData.setValue(null);
        isTranslating = false;
        resetSession();
    }
	
	/**
     * Resets only the {@code isTranslating} guard flag without touching
     * the upsert session state ({@link #lastHistoryId} / {@link #lastSessionTargetLang}).
     *
     * <p>Use this instead of {@link #clearTranslation()} when re-triggering
     * a translation after a mode switch, so the existing history session
     * entry is updated in-place rather than creating a duplicate.</p>
     */
    public void resetTranslatingGuard() {
        isTranslating = false;
        FileLogger.d(TAG, "isTranslating guard reset (session preserved).");
    }

    /**
     * Restores a history item directly into the result LiveData without
     * triggering a new translation or saving a new history entry.
     *
     * <p>Called when the user taps a history chip or history list item.
     * The result is displayed immediately as-is.</p>
     *
     * @param item the history item to restore; must not be null
     */
    public void restoreFromHistory(@NonNull HistoryItem item) {
        final TranslationResult result = new TranslationResult(
                item.getSourceText(),
                item.getTranslatedText(),
                item.getSourceLanguageCode(),
                item.getTargetLanguageCode(),
                item.getTimestamp()
        );
        // Restore language pair to match the history item
        sourceLanguageLiveData.setValue(item.getSourceLanguageCode());
        targetLanguageLiveData.setValue(item.getTargetLanguageCode());
        // Publish result directly — no translate, no history write
        translationResultLiveData.setValue(Resource.success(result));
        // Sync session so editing this text will UPDATE the existing entry
        lastHistoryId = item.getId();
        lastSessionTargetLang = item.getTargetLanguageCode();
        FileLogger.d(TAG, "Restored from history id=" + item.getId()
            + " text=" + item.getSourceText());
    }
	
	/**
     * Clears the one-shot added-to-favorite event after it has been consumed by the UI.
     */
    public void clearAddedToFavorite() {
        addedToFavoriteLiveData.setValue(null);
    }

    /**
     * Adds the given {@link TranslationResult} to the user's favorites.
     *
     * <p>Emits {@code true} via {@link #getAddedToFavoriteLiveData()}
     * after successful insertion.</p>
     *
     * @param result the translation result to bookmark; must not be null
     */
    public void addToFavorite(@NonNull TranslationResult result) {
        final FavoriteItem favoriteItem = new FavoriteItem(
                0L,
                result.getSourceText(),
                result.getTranslatedText(),
                result.getSourceLanguageCode(),
                result.getTargetLanguageCode(),
                System.currentTimeMillis()
        );
        addFavoriteUseCase.execute(favoriteItem);
        addedToFavoriteLiveData.setValue(true);
        FileLogger.d(TAG, "Added to favorites: " + result.getSourceText());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Persists the given {@link TranslationResult} to history using an upsert pattern.
     *
     * <h3>Session logic</h3>
     * <ul>
     *   <li><b>New session</b> ({@link #lastHistoryId} == -1 or target language changed):
     *       INSERT a new row; store the returned auto-generated ID in
     *       {@link #lastHistoryId} and record the target language in
     *       {@link #lastSessionTargetLang}.</li>
     *   <li><b>Same session</b> ({@link #lastHistoryId} &gt; 0 and target language unchanged):
     *       UPDATE the existing row with the latest text and timestamp — no new row created.</li>
     * </ul>
     *
     * <p>This replicates Google Translate's history behaviour: however many times
     * the debounce fires within one editing session, only one history entry is created.</p>
     *
     * @param result the translation result to persist; must not be null
     */
    private void saveToHistory(@NonNull TranslationResult result) {
        final String targetLang = result.getTargetLanguageCode();

        // Target language changed → start a new session
        if (!targetLang.equals(lastSessionTargetLang)) {
            resetSession();
        }

        if (lastHistoryId == -1L) {
            // No entry for this session yet — INSERT and capture the generated id
            final HistoryItem item = new HistoryItem(
                    0L,
                    result.getSourceText(),
                    result.getTranslatedText(),
                    result.getSourceLanguageCode(),
                    targetLang,
                    result.getTimestamp()
            );
            addHistoryUseCase.insertAndGetId(item, generatedId -> {
                lastHistoryId = generatedId;
                lastSessionTargetLang = targetLang;
                FileLogger.d(TAG, "History INSERT id=" + generatedId
                        + " text=" + result.getSourceText());
            });
        } else {
            // Session already has an entry — UPDATE in-place
            updateHistoryUseCase.execute(
                    lastHistoryId,
                    result.getSourceText(),
                    result.getTranslatedText(),
                    result.getSourceLanguageCode(),
                    result.getTimestamp()
            );
            FileLogger.d(TAG, "History UPDATE id=" + lastHistoryId
                    + " text=" + result.getSourceText());
        }
    }

    /**
     * Resets the current translate session state.
     *
     * <p>After this call, the next successful translation will INSERT a fresh
     * history entry rather than updating the previous session's row.</p>
     *
     * <p>Called from:</p>
     * <ul>
     *   <li>{@link #clearTranslation()} — user tapped the clear button</li>
     *   <li>{@link #saveToHistory} — target language changed mid-session</li>
     * </ul>
     */
    private void resetSession() {
        lastHistoryId = -1L;
        lastSessionTargetLang = null;
        FileLogger.d(TAG, "Translate session reset.");
    }

    // -------------------------------------------------------------------------
    // LiveData accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link LiveData} emitting the current translation result state.
     *
     * @return {@link LiveData} of {@link Resource} wrapped {@link TranslationResult}
     */
    @NonNull
    public LiveData<Resource<TranslationResult>> getTranslationResultLiveData() {
        return translationResultLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting the detected source language code.
     *
     * @return {@link LiveData} of {@link Resource} wrapped BCP-47 language code string
     */
    @NonNull
    public LiveData<Resource<String>> getDetectedLanguageLiveData() {
        return detectedLanguageLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting the currently selected source language code.
     *
     * @return {@link LiveData} of BCP-47 source language code string
     */
    @NonNull
    public LiveData<String> getSourceLanguageLiveData() {
        return sourceLanguageLiveData;
    }

    /**
     * Returns the {@link LiveData} emitting the currently selected target language code.
     *
     * @return {@link LiveData} of BCP-47 target language code string
     */
    @NonNull
    public LiveData<String> getTargetLanguageLiveData() {
        return targetLanguageLiveData;
    }

    /**
     * Returns the {@link LiveData} that emits {@code true} when a translation
     * has been successfully added to favorites.
     *
     * <p>This is a one-shot event. The observer should reset this after consuming.</p>
     *
     * @return {@link LiveData} of {@link Boolean} favorite-added event
     */
    @NonNull
    public LiveData<Boolean> getAddedToFavoriteLiveData() {
        return addedToFavoriteLiveData;
    }
	
	public void setOnlineMode(boolean isOnline) {
        isOnlineModeLiveData.setValue(isOnline);
        FileLogger.d(TAG, "Online mode set to: " + isOnline);
    }

    @NonNull
     public LiveData<Boolean> getIsOnlineModeLiveData() {
        return isOnlineModeLiveData;
     }
}
