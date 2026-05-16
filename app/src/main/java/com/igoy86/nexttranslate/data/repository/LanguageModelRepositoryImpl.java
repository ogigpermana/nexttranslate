package com.igoy86.nexttranslate.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.igoy86.nexttranslate.domain.model.DownloadProgress;
import com.igoy86.nexttranslate.domain.model.LanguageModel;
import com.igoy86.nexttranslate.domain.repository.LanguageModelRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Concrete implementation of {@link LanguageModelRepository}.
 *
 * <p>Handles ML Kit language model download, deletion, and status queries
 * using the {@link RemoteModelManager} API.</p>
 *
 * <p>Note: ML Kit's {@code downloadModelIfNeeded()} does not expose real
 * byte-level progress. This implementation uses a {@link Handler}-based
 * ticker to simulate progress in the UI while the real download runs in
 * the background. The ticker stops and snaps to 100 % when the download
 * succeeds or fails.</p>
 *
 * <p>Supported languages are defined in {@link #SUPPORTED_LANGUAGES}.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class LanguageModelRepositoryImpl implements LanguageModelRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "LanguageModelRepoImpl";

    /**
     * Approximate total model size in bytes used for the simulated progress bar.
     * ML Kit translation models are roughly 30–60 MB; 57 MB is a safe upper bound.
     */
    private static final long FAKE_TOTAL_BYTES = 57L * 1024 * 1024; // 57 MB

    /**
     * How many bytes the simulated progress advances every tick.
     * At 500 ms per tick this gives ~2 MB/s visible speed, which looks
     * realistic on a typical mobile connection.
     */
    private static final long FAKE_BYTES_PER_TICK = 2L * 1024 * 1024; // 2 MB

    /** Interval between simulated progress ticks in milliseconds. */
    private static final long TICK_INTERVAL_MS = 500;

    /**
     * The complete list of languages supported by this application.
     * Each entry is a two-element array: {@code [BCP-47 code, Display Name]}.
     */
    private static final String[][] SUPPORTED_LANGUAGES = {
            {"af", "Afrikaans"},
            {"ar", "Arabic"},
            {"be", "Belarusian"},
            {"bg", "Bulgarian"},
            {"bn", "Bengali"},
            {"ca", "Catalan"},
            {"cs", "Czech"},
            {"cy", "Welsh"},
            {"da", "Danish"},
            {"de", "German"},
            {"el", "Greek"},
            {"en", "English"},
            {"eo", "Esperanto"},
            {"es", "Spanish"},
            {"et", "Estonian"},
            {"fa", "Persian"},
            {"fi", "Finnish"},
            {"fr", "French"},
            {"ga", "Irish"},
            {"gl", "Galician"},
            {"gu", "Gujarati"},
            {"he", "Hebrew"},
            {"hi", "Hindi"},
            {"hr", "Croatian"},
            {"ht", "Haitian Creole"},
            {"hu", "Hungarian"},
            {"id", "Indonesian"},
            {"is", "Icelandic"},
            {"it", "Italian"},
            {"ja", "Japanese"},
            {"ka", "Georgian"},
            {"kn", "Kannada"},
            {"ko", "Korean"},
            {"lt", "Lithuanian"},
            {"lv", "Latvian"},
            {"mk", "Macedonian"},
            {"mr", "Marathi"},
            {"ms", "Malay"},
            {"mt", "Maltese"},
            {"nl", "Dutch"},
            {"no", "Norwegian"},
            {"pl", "Polish"},
            {"pt", "Portuguese"},
            {"ro", "Romanian"},
            {"ru", "Russian"},
            {"sk", "Slovak"},
            {"sl", "Slovenian"},
            {"sq", "Albanian"},
            {"sv", "Swedish"},
            {"sw", "Swahili"},
            {"ta", "Tamil"},
            {"te", "Telugu"},
            {"th", "Thai"},
            {"tl", "Filipino"},
            {"tr", "Turkish"},
            {"uk", "Ukrainian"},
            {"ur", "Urdu"},
            {"vi", "Vietnamese"},
            {"zh", "Chinese"},
    };

    @NonNull
    private final AppExecutors appExecutors;

    @NonNull
    private final RemoteModelManager remoteModelManager;

    public LanguageModelRepositoryImpl(@NonNull AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        this.remoteModelManager = RemoteModelManager.getInstance();
    }

    // -------------------------------------------------------------------------
    // downloadModel
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Validates the BCP-47 code, then delegates to
     * {@link Translator#downloadModelIfNeeded}. While the download runs, a
     * {@link Handler} ticker emits simulated {@link Resource#progress} events
     * every {@value #TICK_INTERVAL_MS} ms so the UI can show a moving
     * progress bar. When the real download finishes (success or failure) the
     * ticker is stopped immediately.</p>
     */
    @NonNull
    @Override
    public LiveData<Resource<Object>> downloadModel(
            @NonNull String languageCode,
            boolean requireWifi
    ) {
        final MutableLiveData<Resource<Object>> resultLiveData = new MutableLiveData<>();

        // Emit LOADING immediately — UI shows spinner before first tick arrives.
        resultLiveData.setValue(Resource.loading(null));

        // Validate BCP-47 code — ML Kit silently ignores unknown codes.
        final String mlKitCode = TranslateLanguage.fromLanguageTag(languageCode);
        if (mlKitCode == null) {
            FileLogger.e(TAG, "Unsupported language code: " + languageCode);
            resultLiveData.setValue(Resource.error("Unsupported language: " + languageCode, null));
            return resultLiveData;
        }

        final boolean isEnglish = mlKitCode.equals(TranslateLanguage.ENGLISH);
        final Translator translator = Translation.getClient(
                new TranslatorOptions.Builder()
                        .setSourceLanguage(isEnglish ? TranslateLanguage.INDONESIAN : mlKitCode)
                        .setTargetLanguage(isEnglish ? TranslateLanguage.INDONESIAN : TranslateLanguage.ENGLISH)
                        .build()
        );

        final DownloadConditions.Builder condBuilder = new DownloadConditions.Builder();
        if (requireWifi) condBuilder.requireWifi();
        final DownloadConditions conditions = condBuilder.build();

        final TranslateRemoteModel remoteModel =
                new TranslateRemoteModel.Builder(mlKitCode).build();

        // ── Simulated progress ticker ──────────────────────────────────────
        final Handler handler = new Handler(Looper.getMainLooper());
        final long[] fakeBytes = {0L};
        final boolean[] done = {false};

        final Runnable ticker = new Runnable() {
            @Override
            public void run() {
                if (done[0]) return;
                // Advance but never reach 100 % — the real callback snaps to full.
                fakeBytes[0] = Math.min(
                        fakeBytes[0] + FAKE_BYTES_PER_TICK,
                        FAKE_TOTAL_BYTES - FAKE_BYTES_PER_TICK
                );
                resultLiveData.postValue(Resource.progress(
                        new DownloadProgress(languageCode, fakeBytes[0], FAKE_TOTAL_BYTES)
                ));
                FileLogger.d(TAG, "Simulated progress [" + languageCode + "]: "
                        + fakeBytes[0] / (1024 * 1024) + " MB / "
                        + FAKE_TOTAL_BYTES / (1024 * 1024) + " MB");
                handler.postDelayed(this, TICK_INTERVAL_MS);
            }
        };
		
		FileLogger.i(TAG, "Cleaning partial model before download: " + languageCode);

        remoteModelManager.deleteDownloadedModel(remoteModel)
        .addOnCompleteListener(deleteTask -> {
            FileLogger.i(TAG, "Starting model download: " + languageCode
                    + " (requireWifi=" + requireWifi + ")");
            handler.postDelayed(ticker, TICK_INTERVAL_MS);

            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(unused -> {
                        done[0] = true;
                        handler.removeCallbacksAndMessages(null);
                        FileLogger.i(TAG, "Model downloaded successfully: " + languageCode);
                        resultLiveData.postValue(Resource.progress(
                                new DownloadProgress(languageCode, FAKE_TOTAL_BYTES, FAKE_TOTAL_BYTES)
                        ));
                        handler.postDelayed(
                                () -> resultLiveData.postValue(Resource.success(languageCode)),
                                300
                        );
                        translator.close();
                    })
                    .addOnFailureListener(ex -> {
                        done[0] = true;
                        handler.removeCallbacksAndMessages(null);
                        FileLogger.e(TAG, "Model download failed: " + languageCode, ex);
                        resultLiveData.postValue(
                                Resource.error("Download failed: " + ex.getMessage(), null)
                        );
                        translator.close();
                    });
        });

        return resultLiveData;
    }

    // -------------------------------------------------------------------------
    // deleteModel
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public LiveData<Resource<String>> deleteModel(@NonNull String languageCode) {
        final MutableLiveData<Resource<String>> resultLiveData = new MutableLiveData<>();

        resultLiveData.setValue(Resource.loading(null));

        final String mlKitCode = TranslateLanguage.fromLanguageTag(languageCode);
        if (mlKitCode == null) {
            resultLiveData.setValue(Resource.error("Unsupported language: " + languageCode, null));
            return resultLiveData;
        }

        final TranslateRemoteModel model =
                new TranslateRemoteModel.Builder(mlKitCode).build();

        remoteModelManager.deleteDownloadedModel(model)
                .addOnSuccessListener(unused -> {
                    FileLogger.i(TAG, "Model deleted: " + languageCode);
                    resultLiveData.postValue(Resource.success(languageCode));
                })
                .addOnFailureListener(ex -> {
                    FileLogger.e(TAG, "Model deletion failed: " + languageCode, ex);
                    resultLiveData.postValue(
                            Resource.error("Delete failed: " + ex.getMessage(), null)
                    );
                });

        return resultLiveData;
    }

    // -------------------------------------------------------------------------
    // getDownloadedLanguages
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    public LiveData<Resource<List<LanguageModel>>> getDownloadedLanguages() {
        final MutableLiveData<Resource<List<LanguageModel>>> resultLiveData =
                new MutableLiveData<>();

        resultLiveData.setValue(Resource.loading(null));

        remoteModelManager.getDownloadedModels(TranslateRemoteModel.class)
                .addOnSuccessListener(downloadedModels -> {
                    final Set<TranslateRemoteModel> modelSet = downloadedModels;
                    final List<String> downloadedCodes = new ArrayList<>();

                    for (TranslateRemoteModel m : modelSet) {
                        downloadedCodes.add(m.getLanguage());
                    }

                    final List<LanguageModel> languageModels = new ArrayList<>();
                    for (String[] language : SUPPORTED_LANGUAGES) {
                        final String code = language[0];
                        final String displayName = language[1];
                        final boolean isDownloaded = downloadedCodes.contains(code);
                        final boolean isDefault = code.equals("en");
                        languageModels.add(new LanguageModel(code, displayName, isDownloaded, isDefault));
                    }

                    FileLogger.d(TAG, "Downloaded models fetched. Count: "
                            + downloadedCodes.size());
                    resultLiveData.postValue(Resource.success(languageModels));
                })
                .addOnFailureListener(ex -> {
                    FileLogger.e(TAG, "Failed to fetch downloaded models.", ex);
                    resultLiveData.postValue(
                            Resource.error("Failed to load languages: " + ex.getMessage(), null)
                    );
                });

        return resultLiveData;
    }
}

