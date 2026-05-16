package com.igoy86.nexttranslate;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.igoy86.nexttranslate.databinding.ActivityMainBinding;
import com.igoy86.nexttranslate.presentation.base.BaseActivity;
import com.igoy86.nexttranslate.presentation.favorite.FavoriteFragment;
import com.igoy86.nexttranslate.presentation.history.HistoryFragment;
import com.igoy86.nexttranslate.presentation.language.LanguageFragment;
import com.igoy86.nexttranslate.presentation.translate.TranslateFragment;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * The single host Activity for NextTranslate.
 *
 * <p>Implements a single-Activity architecture where all screens are
 * represented as {@link Fragment} instances swapped inside
 * {@code R.id.fragment_container} using the FragmentManager.</p>
 *
 * <p>Navigation flow:</p>
 * <ul>
 *     <li>{@link TranslateFragment}  — root screen, loaded on fresh launch</li>
 *     <li>{@link HistoryFragment}    — opened via history icon in TranslateFragment</li>
 *     <li>{@link FavoriteFragment}   — opened via favorites icon in TranslateFragment</li>
 *     <li>{@link LanguageFragment}   — opened via language/download icon in TranslateFragment</li>
 * </ul>
 *
 * <p>Fragment navigation is initiated from child Fragments by casting
 * {@code requireActivity()} to {@link MainActivity} and calling the
 * appropriate {@code open*()} method.</p>
 *
 * <p>Example from a Fragment:</p>
 * <pre>
 *     ((MainActivity) requireActivity()).openHistoryFragment();
 * </pre>
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Tag used for logging events originating from this Activity. */
    private static final String TAG = "MainActivity";

    /** Back-stack tag for {@link HistoryFragment}. */
    public static final String TAG_HISTORY  = "HistoryFragment";

    /** Back-stack tag for {@link FavoriteFragment}. */
    public static final String TAG_FAVORITE = "FavoriteFragment";

    /** Back-stack tag for {@link LanguageFragment}. */
    public static final String TAG_LANGUAGE = "LanguageFragment";

    // -------------------------------------------------------------------------
    // BaseActivity contract
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Inflates {@code activity_main.xml} using ViewBinding.</p>
     */
    @NonNull
    @Override
    protected ActivityMainBinding initBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads {@link TranslateFragment} as the root screen on a fresh launch.
     * On configuration changes the FragmentManager restores the stack
     * automatically, so we skip re-loading in that case.</p>
     */
    @Override
    protected void initViews() {
        if (getSupportFragmentManager().findFragmentById(
                binding.fragmentContainer.getId()) == null) {
            loadRootFragment();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>No Activity-level LiveData to observe in this host Activity.
     * Each Fragment observes its own ViewModel independently.</p>
     */
    @Override
    protected void initObservers() {
        // No-op: observers are handled inside each Fragment
    }

    /**
     * {@inheritDoc}
     *
     * <p>No Activity-level click listeners. All interactions are
     * handled inside individual Fragments.</p>
     */
    @Override
    protected void initListeners() {
        // No-op: listeners are handled inside each Fragment
    }

    // -------------------------------------------------------------------------
    // Navigation helpers (called by child Fragments)
    // -------------------------------------------------------------------------

    /**
     * Navigates to {@link HistoryFragment}.
     *
     * <p>If an instance already exists in the back stack, it is popped
     * to the top instead of creating a duplicate.</p>
     */
    public void openHistoryFragment() {
        FileLogger.d(TAG, "Opening HistoryFragment.");
        openFragment(new HistoryFragment(), TAG_HISTORY);
    }

    /**
     * Navigates to {@link FavoriteFragment}.
     *
     * <p>If an instance already exists in the back stack, it is popped
     * to the top instead of creating a duplicate.</p>
     */
    public void openFavoriteFragment() {
        FileLogger.d(TAG, "Opening FavoriteFragment.");
        openFragment(new FavoriteFragment(), TAG_FAVORITE);
    }

    /**
     * Navigates to {@link LanguageFragment}.
     *
     * <p>If an instance already exists in the back stack, it is popped
     * to the top instead of creating a duplicate.</p>
     */
    public void openLanguageFragment() {
        FileLogger.d(TAG, "Opening LanguageFragment.");
        openFragment(new LanguageFragment(), TAG_LANGUAGE);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads {@link TranslateFragment} as the non-back-stackable root fragment.
     *
     * <p>The root fragment is never added to the back stack so that pressing
     * Back from it exits the app naturally.</p>
     */
    private void loadRootFragment() {
        FileLogger.d(TAG, "Loading root TranslateFragment.");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), new TranslateFragment())
                .commit();
    }

    /**
     * Opens a fragment by replacing the current container content.
     *
     * <p>Checks if the fragment with the given tag already exists in the back
     * stack — if so, pops to it to avoid duplicate instances. Otherwise,
     * performs a fresh {@code replace} transaction with slide animations.</p>
     *
     * @param fragment the {@link Fragment} instance to display
     * @param tag      unique back-stack tag identifying this fragment type
     */
    private void openFragment(@NonNull Fragment fragment, @NonNull String tag) {
        FragmentManager fm = getSupportFragmentManager();

        // Avoid duplicate — pop to existing instance if already in stack
        boolean popped = fm.popBackStackImmediate(tag, 0);
        if (popped) {
            FileLogger.d(TAG, "Popped to existing fragment: " + tag);
            return;
        }

        // Not in stack — create a fresh transaction
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.slide_in_left,    // enter
                android.R.anim.fade_out,          // exit
                android.R.anim.fade_in,           // pop enter
                android.R.anim.slide_out_right    // pop exit
        );
        transaction.replace(binding.fragmentContainer.getId(), fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commit();

        FileLogger.d(TAG, "Fragment transaction committed: " + tag);
    }
}