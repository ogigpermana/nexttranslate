package com.igoy86.nexttranslate;

import android.app.Application;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.UserSession;

/**
 * Custom {@link Application} class for the NextTranslate application.
 *
 * <p>Serves as the entry point for application-level initialization.
 * Responsibilities include:</p>
 * <ul>
 *     <li>Initializing {@link FileLogger} for file-based debug logging</li>
 *     <li>Creating and holding the {@link AppContainer} singleton for
 *         manual dependency injection</li>
 * </ul>
 *
 * <p>This class must be registered in {@code AndroidManifest.xml}:</p>
 * <pre>
 *     {@literal <}application
 *         android:name=".NextTranslateApp"
 *         ... {@literal >}
 * </pre>
 *
 * <p>Access the {@link AppContainer} from anywhere in the app:</p>
 * <pre>
 *     AppContainer container = NextTranslateApp.getContainer();
 *     TranslateTextUseCase useCase = container.getTranslateTextUseCase();
 * </pre>
 */
public class NextTranslateApp extends Application {

    /** Tag used for logging events originating from this class. */
    private static final String TAG = "NextTranslateApp";

    /**
     * The global dependency injection container.
     * Holds all application-scoped dependencies.
     */
    private static AppContainer container;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called when the application is starting, before any Activity, Service,
     * or BroadcastReceiver has been created.
     *
     * <p>Initializes all application-level components in the following order:</p>
     * <ol>
     *     <li>{@link FileLogger} — must be first so all subsequent logs are captured</li>
     *     <li>{@link AppContainer} — builds the full dependency graph</li>
     * </ol>
     */
    @Override
    public void onCreate() {
        super.onCreate();
		
		// Apply saved theme on app start
        final int savedMode = UserSession.getInstance(this).getThemeMode();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedMode);
		
        initFileLogger();
        initAppContainer();

        FileLogger.i(TAG, "NextTranslateApp started successfully.");
        FileLogger.i(TAG, "Package: " + getPackageName());
        FileLogger.i(TAG, "Version: " + BuildConfig.VERSION_NAME
                + " (" + BuildConfig.VERSION_CODE + ")");
    }

    // -------------------------------------------------------------------------
    // Private initialization helpers
    // -------------------------------------------------------------------------

    /**
     * Initializes the {@link FileLogger} utility.
     *
     * <p>Must be called before any other component so that all subsequent
     * log entries are captured to the log file.</p>
     */
    private void initFileLogger() {
        FileLogger.init(this);
        FileLogger.i(TAG, "FileLogger initialized.");
    }

    /**
     * Initializes the {@link AppContainer} with the application context.
     *
     * <p>Builds the full dependency graph including the Room database,
     * all repositories, and all use cases.</p>
     */
    private void initAppContainer() {
        container = new AppContainer(this);
        FileLogger.i(TAG, "AppContainer initialized.");
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the global {@link AppContainer} instance.
     *
     * <p>Use this to access use cases and repositories from ViewModels
     * or ViewModel factories:</p>
     * <pre>
     *     AppContainer container = NextTranslateApp.getContainer();
     * </pre>
     *
     * @return the application-scoped {@link AppContainer}
     * @throws IllegalStateException if called before {@link #onCreate()} completes
     */
    @NonNull
    public static AppContainer getContainer() {
        if (container == null) {
            throw new IllegalStateException(
                    "AppContainer is not initialized. " +
                    "Ensure NextTranslateApp is registered in AndroidManifest.xml " +
                    "via android:name=\".NextTranslateApp\"."
            );
        }
        return container;
    }
}