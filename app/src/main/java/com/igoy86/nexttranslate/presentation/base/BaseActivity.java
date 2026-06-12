package com.igoy86.nexttranslate.presentation.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.snackbar.Snackbar;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Base class for all Activity implementations in the NextTranslate application.
 *
 * <p>Provides common functionality shared across all Activities, including:</p>
 * <ul>
 *     <li>ViewBinding initialization via the abstract {@link #initBinding()} method</li>
 *     <li>Standardized UI setup hooks ({@link #initViews()}, {@link #initObservers()},
 *         {@link #initListeners()})</li>
 *     <li>Convenience methods for showing Toast and Snackbar messages</li>
 *     <li>Centralized lifecycle logging via {@link FileLogger}</li>
 * </ul>
 *
 * <p>All Activities in this project should extend this class instead of
 * extending {@link AppCompatActivity} directly.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     public class MainActivity extends BaseActivity{@literal <}ActivityMainBinding{@literal >} {
 *
 *         {@literal @}Override
 *         protected ActivityMainBinding initBinding() {
 *             return ActivityMainBinding.inflate(getLayoutInflater());
 *         }
 *
 *         {@literal @}Override
 *         protected void initViews() {
 *             // setup views here
 *         }
 *
 *         {@literal @}Override
 *         protected void initObservers() {
 *             // observe LiveData here
 *         }
 *
 *         {@literal @}Override
 *         protected void initListeners() {
 *             // set click listeners here
 *         }
 *     }
 * </pre>
 *
 * @param <VB> the type of {@link ViewBinding} used by this Activity
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {

    /** Tag used for logging lifecycle events of this Activity. */
    private static final String TAG = "BaseActivity";

    /**
     * The ViewBinding instance for this Activity.
     * Accessible by subclasses to interact with views.
     */
    protected VB binding;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called when the Activity is first created.
     *
     * <p>Inflates the ViewBinding, sets the content view, and invokes
     * the setup hooks in the following order:</p>
     * <ol>
     *     <li>{@link #initViews()}</li>
     *     <li>{@link #initObservers()}</li>
     *     <li>{@link #initListeners()}</li>
     * </ol>
     *
     * @param savedInstanceState if the Activity is being re-created from a
     *                           previous saved state, this bundle contains the data;
     *                           otherwise {@code null}
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allow fragments to draw behind status bar and nav bar,
        // so each fragment can handle its own window insets.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = initBinding();
        setContentView(binding.getRoot());

        FileLogger.d(TAG, getClass().getSimpleName() + " onCreate.");

        initViews();
        initObservers();
        initListeners();
    }

    /**
     * Called when the Activity becomes visible to the user.
     */
    @Override
    protected void onStart() {
        super.onStart();
        FileLogger.d(TAG, getClass().getSimpleName() + " onStart.");
    }

    /**
     * Called when the Activity starts interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
        FileLogger.d(TAG, getClass().getSimpleName() + " onResume.");
    }

    /**
     * Called when the Activity is no longer interacting with the user.
     */
    @Override
    protected void onPause() {
        super.onPause();
        FileLogger.d(TAG, getClass().getSimpleName() + " onPause.");
    }

    /**
     * Called when the Activity is no longer visible to the user.
     */
    @Override
    protected void onStop() {
        super.onStop();
        FileLogger.d(TAG, getClass().getSimpleName() + " onStop.");
    }

    /**
     * Called before the Activity is destroyed.
     *
     * <p>Releases the ViewBinding reference to prevent memory leaks.</p>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileLogger.d(TAG, getClass().getSimpleName() + " onDestroy.");
        binding = null;
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    /**
     * Inflates and returns the {@link ViewBinding} for this Activity.
     *
     * <p>Implement this method by returning the inflated binding:</p>
     * <pre>
     *     return ActivityMainBinding.inflate(getLayoutInflater());
     * </pre>
     *
     * @return the inflated {@link ViewBinding} instance
     */
    @NonNull
    protected abstract VB initBinding();

    /**
     * Called after {@link #initBinding()} to initialize and configure views.
     *
     * <p>Override this method to set up RecyclerView adapters, configure
     * Toolbar, or perform any one-time view initialization.</p>
     */
    protected abstract void initViews();

    /**
     * Called after {@link #initViews()} to set up LiveData observers.
     *
     * <p>Override this method to observe {@link androidx.lifecycle.LiveData}
     * from your ViewModel and react to state changes.</p>
     */
    protected abstract void initObservers();

    /**
     * Called after {@link #initObservers()} to attach event listeners.
     *
     * <p>Override this method to set click listeners, text watchers,
     * or any other user interaction handlers.</p>
     */
    protected abstract void initListeners();

    // -------------------------------------------------------------------------
    // UI helper methods
    // -------------------------------------------------------------------------

    /**
     * Displays a short-duration {@link Toast} message.
     *
     * @param message the message string to display
     */
    protected void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a short-duration {@link Toast} message from a string resource.
     *
     * @param resId the string resource ID of the message to display
     */
    protected void showToast(@StringRes int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a {@link Snackbar} with a short duration anchored to the
     * root view of this Activity.
     *
     * @param message the message string to display in the Snackbar
     */
    protected void showSnackbar(@NonNull String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Displays a {@link Snackbar} with an action button anchored to the
     * root view of this Activity.
     *
     * @param message        the message string to display
     * @param actionLabel    the label for the action button
     * @param actionListener the click listener for the action button
     */
    protected void showSnackbarWithAction(
            @NonNull String message,
            @NonNull String actionLabel,
            @NonNull View.OnClickListener actionListener
    ) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction(actionLabel, actionListener)
                .show();
    }

    /**
     * Displays a {@link Snackbar} with a short duration from a string resource.
     *
     * @param resId the string resource ID of the message to display
     */
    protected void showSnackbar(@StringRes int resId) {
        Snackbar.make(binding.getRoot(), resId, Snackbar.LENGTH_SHORT).show();
    }
}