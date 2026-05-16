package com.igoy86.nexttranslate.presentation.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.snackbar.Snackbar;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Base class for all Fragment implementations in the NextTranslate application.
 *
 * <p>Provides common functionality shared across all Fragments, including:</p>
 * <ul>
 *     <li>ViewBinding initialization and automatic cleanup to prevent memory leaks</li>
 *     <li>Standardized UI setup hooks ({@link #initViews()}, {@link #initObservers()},
 *         {@link #initListeners()})</li>
 *     <li>Convenience methods for showing Toast and Snackbar messages</li>
 *     <li>Centralized lifecycle logging via {@link FileLogger}</li>
 * </ul>
 *
 * <p>All Fragments in this project should extend this class instead of
 * extending {@link Fragment} directly.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     public class TranslateFragment extends BaseFragment{@literal <}FragmentTranslateBinding{@literal >} {
 *
 *         {@literal @}Override
 *         protected FragmentTranslateBinding initBinding(
 *                 {@literal @}NonNull LayoutInflater inflater,
 *                 {@literal @}Nullable ViewGroup container) {
 *             return FragmentTranslateBinding.inflate(inflater, container, false);
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
 * @param <VB> the type of {@link ViewBinding} used by this Fragment
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    /** Tag used for logging lifecycle events of this Fragment. */
    private static final String TAG = "BaseFragment";

    /**
     * The ViewBinding instance for this Fragment.
     *
     * <p>Only valid between {@link #onCreateView} and {@link #onDestroyView}.
     * Accessing this field outside that range will result in a
     * {@link NullPointerException}.</p>
     */
    private VB binding;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called to inflate the Fragment's view hierarchy.
     *
     * <p>Delegates view inflation to {@link #initBinding(LayoutInflater, ViewGroup)}
     * and stores the resulting binding instance.</p>
     *
     * @param inflater           the LayoutInflater used to inflate views
     * @param container          the parent view the fragment's UI will attach to
     * @param savedInstanceState previously saved state, or {@code null}
     * @return the root {@link View} of the inflated layout
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = initBinding(inflater, container);
        FileLogger.d(TAG, getClass().getSimpleName() + " onCreateView.");
        return binding.getRoot();
    }

    /**
     * Called immediately after {@link #onCreateView} has returned.
     *
     * <p>Invokes the setup hooks in the following order:</p>
     * <ol>
     *     <li>{@link #initViews()}</li>
     *     <li>{@link #initObservers()}</li>
     *     <li>{@link #initListeners()}</li>
     * </ol>
     *
     * @param view               the root View returned by {@link #onCreateView}
     * @param savedInstanceState previously saved state, or {@code null}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FileLogger.d(TAG, getClass().getSimpleName() + " onViewCreated.");
        initViews();
        initObservers();
        initListeners();
    }

    /**
     * Called when the Fragment becomes visible to the user.
     */
    @Override
    public void onStart() {
        super.onStart();
        FileLogger.d(TAG, getClass().getSimpleName() + " onStart.");
    }

    /**
     * Called when the Fragment starts interacting with the user.
     */
    @Override
    public void onResume() {
        super.onResume();
        FileLogger.d(TAG, getClass().getSimpleName() + " onResume.");
    }

    /**
     * Called when the Fragment is no longer interacting with the user.
     */
    @Override
    public void onPause() {
        super.onPause();
        FileLogger.d(TAG, getClass().getSimpleName() + " onPause.");
    }

    /**
     * Called when the Fragment is no longer visible to the user.
     */
    @Override
    public void onStop() {
        super.onStop();
        FileLogger.d(TAG, getClass().getSimpleName() + " onStop.");
    }

    /**
     * Called when the view hierarchy associated with the Fragment is destroyed.
     *
     * <p>Sets the binding reference to {@code null} to prevent memory leaks.
     * Subclasses must not access {@link #getBinding()} after this point.</p>
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FileLogger.d(TAG, getClass().getSimpleName() + " onDestroyView.");
        binding = null;
    }

    /**
     * Called when the Fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        FileLogger.d(TAG, getClass().getSimpleName() + " onDestroy.");
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    /**
     * Inflates and returns the {@link ViewBinding} for this Fragment.
     *
     * <p>Implement this method by returning the inflated binding:</p>
     * <pre>
     *     return FragmentTranslateBinding.inflate(inflater, container, false);
     * </pre>
     *
     * @param inflater  the LayoutInflater used to inflate the layout
     * @param container the parent ViewGroup, or {@code null}
     * @return the inflated {@link ViewBinding} instance
     */
    @NonNull
    protected abstract VB initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    );

    /**
     * Called after {@link #onViewCreated} to initialize and configure views.
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
    // Protected accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the ViewBinding instance associated with this Fragment.
     *
     * <p><strong>Important:</strong> Only call this between {@link #onCreateView}
     * and {@link #onDestroyView}. Accessing outside this range will throw
     * an {@link IllegalStateException}.</p>
     *
     * @return the current {@link ViewBinding} instance
     * @throws IllegalStateException if called before onCreateView or after onDestroyView
     */
    @NonNull
    protected VB getBinding() {
        if (binding == null) {
            throw new IllegalStateException(
                    "Binding is null. Ensure getBinding() is only called between " +
                    "onCreateView() and onDestroyView()."
            );
        }
        return binding;
    }

    // -------------------------------------------------------------------------
    // UI helper methods
    // -------------------------------------------------------------------------

    /**
     * Displays a short-duration {@link Toast} message.
     *
     * @param message the message string to display
     */
    protected void showToast(@NonNull String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a short-duration {@link Toast} message from a string resource.
     *
     * @param resId the string resource ID of the message to display
     */
    protected void showToast(@StringRes int resId) {
        if (getContext() != null) {
            Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a {@link Snackbar} with a short duration anchored to the
     * root view of this Fragment.
     *
     * @param message the message string to display in the Snackbar
     */
    protected void showSnackbar(@NonNull String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a {@link Snackbar} with an action button anchored to the
     * root view of this Fragment.
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
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setAction(actionLabel, actionListener)
                    .show();
        }
    }

    /**
     * Displays a {@link Snackbar} with a short duration from a string resource.
     *
     * @param resId the string resource ID of the message to display
     */
    protected void showSnackbar(@StringRes int resId) {
        if (getView() != null) {
            Snackbar.make(getView(), resId, Snackbar.LENGTH_SHORT).show();
        }
    }
}