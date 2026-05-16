package com.igoy86.nexttranslate.presentation.base;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.Resource;

/**
 * Base class for all {@link ViewModel} implementations in the NextTranslate application.
 *
 * <p>Provides common functionality shared across all ViewModels, including:</p>
 * <ul>
 *     <li>A loading state {@link LiveData} to show/hide progress indicators</li>
 *     <li>An error state {@link LiveData} to propagate error messages to the UI</li>
 *     <li>Helper methods for emitting {@link Resource} states consistently</li>
 *     <li>Centralized error logging via {@link FileLogger}</li>
 * </ul>
 *
 * <p>All ViewModels in this project should extend this class instead of
 * extending {@link ViewModel} directly.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     public class TranslateViewModel extends BaseViewModel {
 *
 *         public void translate(String text) {
 *             setLoading(true);
 *             // ... perform operation
 *             setLoading(false);
 *         }
 *     }
 * </pre>
 */
public abstract class BaseViewModel extends ViewModel {

    /** Tag used for logging events originating from this ViewModel. */
    private static final String TAG = "BaseViewModel";

    /**
     * Backing field for the loading state.
     * {@code true} indicates an ongoing background operation.
     */
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    /**
     * Backing field for the error message state.
     * Emits a non-null message string when an error occurs.
     */
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    // -------------------------------------------------------------------------
    // Exposed LiveData
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link LiveData} that emits {@code true} while a background
     * operation is in progress, and {@code false} when it completes.
     *
     * <p>Observe this in your Activity or Fragment to show/hide a
     * progress bar or loading indicator.</p>
     *
     * @return immutable {@link LiveData} of the loading state
     */
    @NonNull
    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    /**
     * Returns a {@link LiveData} that emits an error message string
     * whenever an operation fails.
     *
     * <p>Observe this in your Activity or Fragment to display
     * a Snackbar, Toast, or error view.</p>
     *
     * @return immutable {@link LiveData} of the error message
     */
    @NonNull
    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    // -------------------------------------------------------------------------
    // Protected helper methods
    // -------------------------------------------------------------------------

    /**
     * Updates the loading state observed by the UI.
     *
     * <p>Must be called from the main thread. Use
     * {@link #postLoading(boolean)} when calling from a background thread.</p>
     *
     * @param isLoading {@code true} to indicate loading; {@code false} to hide
     */
    protected void setLoading(boolean isLoading) {
        loadingLiveData.setValue(isLoading);
    }

    /**
     * Updates the loading state from a background thread.
     *
     * <p>Use this variant when the call originates from an
     * {@link java.util.concurrent.Executor} or worker thread.</p>
     *
     * @param isLoading {@code true} to indicate loading; {@code false} to hide
     */
    protected void postLoading(boolean isLoading) {
        loadingLiveData.postValue(isLoading);
    }

    /**
     * Emits an error message to be observed by the UI.
     *
     * <p>Also writes the error to the log file via {@link FileLogger}.
     * Must be called from the main thread. Use {@link #postError(String)}
     * when calling from a background thread.</p>
     *
     * @param message a human-readable description of the error
     */
    protected void setError(@NonNull String message) {
        FileLogger.e(TAG, "Error emitted: " + message);
        errorLiveData.setValue(message);
    }

    /**
     * Emits an error message from a background thread.
     *
     * <p>Also logs the error and its cause via {@link FileLogger}.</p>
     *
     * @param message   a human-readable description of the error
     * @param throwable the exception that caused the error, for stack trace logging
     */
    protected void postError(@NonNull String message, @NonNull Throwable throwable) {
        FileLogger.e(TAG, "Error emitted: " + message, throwable);
        errorLiveData.postValue(message);
    }

    /**
     * Emits an error message from a background thread without a throwable.
     *
     * @param message a human-readable description of the error
     */
    protected void postError(@NonNull String message) {
        FileLogger.e(TAG, "Error emitted: " + message);
        errorLiveData.postValue(message);
    }

    /**
     * Convenience method that sets loading to {@code false} and emits
     * an error message in a single call.
     *
     * <p>Typically called when a background operation fails, to stop the
     * loading indicator and surface the error to the user simultaneously.</p>
     *
     * @param message   a human-readable description of the error
     * @param throwable the exception that caused the error
     */
    protected void postLoadingAndError(@NonNull String message, @NonNull Throwable throwable) {
        postLoading(false);
        postError(message, throwable);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called when this ViewModel is no longer used and will be destroyed.
     *
     * <p>Subclasses should override this method to cancel any ongoing
     * background tasks or release resources. Always call {@code super.onCleared()}.</p>
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        FileLogger.d(TAG, getClass().getSimpleName() + " cleared.");
    }
}