package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.igoy86.nexttranslate.domain.model.TranslationResult;
import com.igoy86.nexttranslate.domain.repository.TranslateRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Concrete implementation of {@link TranslateRepository}.
 *
 * <p>Handles text translation and language detection using ML Kit Translate
 * and ML Kit Language Identification respectively.</p>
 *
 * <p>Each translation request creates a new {@link Translator} instance
 * configured for the given language pair. The translator is closed after
 * use to release ML Kit resources.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class TranslateRepositoryImpl implements TranslateRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "TranslateRepositoryImpl";

    /**
     * Confidence threshold below which a detected language is considered
     * unreliable by ML Kit Language Identification.
     */
    private static final float LANGUAGE_DETECTION_CONFIDENCE_THRESHOLD = 0.5f;

    /** Executor pools used to dispatch background and main-thread tasks. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link TranslateRepositoryImpl} with the required dependencies.
     *
     * @param appExecutors the executor pools for background threading; must not be null
     */
    public TranslateRepositoryImpl(@NonNull AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // TranslateRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Creates a {@link Translator} for the given language pair and
     * downloads the required model if not already available on the device.
     * The translation is performed asynchronously via ML Kit's Task API.</p>
     *
     * <p>Emits {@link Resource#loading(Object)} immediately, then either
     * {@link Resource#success(Object)} or {@link Resource#error(String, Object)}
     * depending on the result.</p>
     */
    @NonNull
    @Override
    public LiveData<Resource<TranslationResult>> translate(
            @NonNull String sourceText,
            @NonNull String sourceLanguageCode,
            @NonNull String targetLanguageCode
    ) {
        final MutableLiveData<Resource<TranslationResult>> resultLiveData =
                new MutableLiveData<>();

        // Emit loading state immediately
        resultLiveData.setValue(Resource.loading(null));

        final String mlKitSource = TranslateLanguage.fromLanguageTag(sourceLanguageCode);
        final String mlKitTarget = TranslateLanguage.fromLanguageTag(targetLanguageCode);

        if (mlKitSource == null || mlKitTarget == null) {
            FileLogger.e(TAG, "Unsupported language pair: " + sourceLanguageCode + " → " + targetLanguageCode);
            resultLiveData.setValue(
                    Resource.error("Unsupported language pair.", null)
            );
            return resultLiveData;
        }

        final TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(mlKitSource)
                .setTargetLanguage(mlKitTarget)
                .build();

        final Translator translator = Translation.getClient(options);

        final DownloadConditions downloadConditions = new DownloadConditions.Builder()
                .build();

        // Download model if needed, then translate
        translator.downloadModelIfNeeded(downloadConditions)
                .addOnSuccessListener(unused -> {
                    FileLogger.d(TAG, "Model ready. Translating: " + sourceText);

                    translator.translate(sourceText)
                            .addOnSuccessListener(translatedText -> {
                                final TranslationResult result = new TranslationResult(
                                        sourceText,
                                        translatedText,
                                        sourceLanguageCode,
                                        targetLanguageCode,
                                        System.currentTimeMillis()
                                );
                                FileLogger.d(TAG, "Translation success: " + translatedText);
                                resultLiveData.setValue(Resource.success(result));
                                translator.close();
                            })
                            .addOnFailureListener(ex -> {
                                FileLogger.e(TAG, "Translation failed.", ex);
                                resultLiveData.setValue(
                                        Resource.error("Translation failed: " + ex.getMessage(), null)
                                );
                                translator.close();
                            });
                })
                .addOnFailureListener(ex -> {
                    FileLogger.e(TAG, "Model download failed.", ex);
                    resultLiveData.setValue(
                            Resource.error("Model download failed: " + ex.getMessage(), null)
                    );
                    translator.close();
                });

        return resultLiveData;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses ML Kit {@link LanguageIdentifier} to detect the language of
     * the given text. If the detected language code is {@code "und"} (undefined),
     * an error is emitted indicating the language could not be identified.</p>
     */
    @NonNull
    @Override
    public LiveData<Resource<String>> detectLanguage(@NonNull String text) {
        final MutableLiveData<Resource<String>> resultLiveData = new MutableLiveData<>();

        // Emit loading state immediately
        resultLiveData.setValue(Resource.loading(null));

        final LanguageIdentifier languageIdentifier =
                LanguageIdentification.getClient();

        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        // ML Kit returns "und" when language cannot be determined
                        FileLogger.w(TAG, "Language detection returned undefined for: " + text);
                        resultLiveData.setValue(
                                Resource.error("Could not detect language.", null)
                        );
                    } else {
                        FileLogger.d(TAG, "Language detected: " + languageCode);
                        resultLiveData.setValue(Resource.success(languageCode));
                    }
                    languageIdentifier.close();
                })
                .addOnFailureListener(ex -> {
                    FileLogger.e(TAG, "Language detection failed.", ex);
                    resultLiveData.setValue(
                            Resource.error("Language detection failed: " + ex.getMessage(), null)
                    );
                    languageIdentifier.close();
                });

        return resultLiveData;
    }
}