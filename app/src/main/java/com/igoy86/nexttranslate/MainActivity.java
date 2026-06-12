package com.igoy86.nexttranslate;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.igoy86.nexttranslate.databinding.ActivityMainBinding;
import com.igoy86.nexttranslate.presentation.base.BaseActivity;
import com.igoy86.nexttranslate.presentation.collection.CollectionFragment;
import com.igoy86.nexttranslate.presentation.dialog.DialogModeFragment;
import com.igoy86.nexttranslate.presentation.favorite.FavoriteFragment;
import com.igoy86.nexttranslate.presentation.history.HistoryFragment;
import com.igoy86.nexttranslate.presentation.language.LanguageFragment;
import com.igoy86.nexttranslate.presentation.photo.PhotoFragment;
import com.igoy86.nexttranslate.presentation.translate.TranslateFragment;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.presentation.collection.CollectionDetailFragment;

/**
 * The single host Activity for NextTranslate.
 *
 * <p>Implements a single-Activity architecture. All primary screens are
 * {@link Fragment} instances swapped inside {@code R.id.fragment_container}
 * by responding to {@link BottomNavigationView} selections.</p>
 *
 * <p>Tab mapping:</p>
 * <ul>
 *     <li>{@code nav_text}     → {@link TranslateFragment}   — text translation</li>
 *     <li>{@code nav_photo}    → {@link PhotoFragment}        — photo / OCR (placeholder)</li>
 *     <li>{@code nav_favorite} → {@link FavoriteFragment}     — bookmarked translations</li>
 *     <li>{@code nav_dialog}   → {@link DialogModeFragment}   — two-way interpreter (placeholder)</li>
 * </ul>
 *
 * <p>Secondary screens pushed on top of the back stack:</p>
 * <ul>
 *     <li>{@link HistoryFragment}    — full translation history list</li>
 *     <li>{@link CollectionFragment} — user collections (toolbar left icon)</li>
 *     <li>{@link LanguageFragment}   — language pack manager</li>
 * </ul>
 *
 * <p>Toolbar right avatar button → {@link LoginActivity} (guest) or
 * {@link SettingsActivity} (authenticated user). Authentication state
 * is checked via {@code UserSession} — placeholder for now.</p>
 *
 * <p>Fragment navigation initiated from child Fragments:</p>
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

   /** Fragment tag for the root {@link TranslateFragment}. */
    public static final String TAG_TRANSLATE  = "TranslateFragment";

    /** Back-stack tag for {@link HistoryFragment}. */
    public static final String TAG_HISTORY    = "HistoryFragment";

    /** Back-stack tag for {@link FavoriteFragment}. */
    public static final String TAG_FAVORITE   = "FavoriteFragment";

    /** Back-stack tag for {@link LanguageFragment}. */
    public static final String TAG_LANGUAGE   = "LanguageFragment";

    /** Back-stack tag for {@link CollectionFragment}. */
    public static final String TAG_COLLECTION = "CollectionFragment";

    /** Back-stack tag for {@link PhotoFragment}. */
    public static final String TAG_PHOTO      = "PhotoFragment";

    /** Back-stack tag for {@link DialogModeFragment}. */
    public static final String TAG_DIALOG     = "DialogModeFragment";
	
	/** Back-stack tag for {@link CollectionDetailFragment}. */
    public static final String TAG_COLLECTION_DETAIL = "CollectionDetailFragment";

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
     * automatically — skipped in that case.</p>
     *
     * <p>Also wires up the toolbar collection icon and avatar button.</p>
     */
    @Override
    protected void initViews() {
        if (getSupportFragmentManager().findFragmentById(
                binding.fragmentContainer.getId()) == null) {
            loadRootFragment();
        }
        binding.bottomNavigation.setSelectedItemId(R.id.nav_text);

        // Apply system navigation bar inset to bottom nav manually.
        // fitsSystemWindows=false means we handle it here so keyboard
        // does not push the bottom nav up.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
            binding.bottomNavigation, (v, insets) -> {
                final int navBarHeight = insets
                        .getInsets(androidx.core.view.WindowInsetsCompat
                                .Type.navigationBars()).bottom;
                v.setPadding(0, 0, 0, navBarHeight);
                return insets;
            });
    }

    /**
     * {@inheritDoc}
     *
     * <p>No Activity-level LiveData. Each Fragment observes its own
     * ViewModel independently.</p>
     */
    @Override
    protected void initObservers() {
        // No-op
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wires:</p>
     * <ul>
     *     <li>{@link BottomNavigationView} item selections → fragment swaps</li>
     *     <li>Toolbar collection icon → {@link CollectionFragment}</li>
     *     <li>Toolbar avatar button → {@link LoginActivity} or
     *         {@link SettingsActivity}</li>
     * </ul>
     */
    @Override
    protected void initListeners() {
        // Toolbar buttons (btnCollection, btnAvatar) removed from MainActivity.
        // They now live in fragment_translate.xml and are wired in TranslateFragment.
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_text) {
                FileLogger.d(TAG, "Tab selected: Text");
                switchTab(new TranslateFragment(), TAG_TRANSLATE); 
                return true;
            }
            if (id == R.id.nav_photo) {
                FileLogger.d(TAG, "Tab selected: Photo");
                switchTab(new PhotoFragment(), TAG_PHOTO);
                return true;
            }
            if (id == R.id.nav_favorite) {
                FileLogger.d(TAG, "Tab selected: Favorite");
                switchTab(new FavoriteFragment(), TAG_FAVORITE);
                return true;
            }
            if (id == R.id.nav_dialog) {
                FileLogger.d(TAG, "Tab selected: Dialog");
                switchTab(new DialogModeFragment(), TAG_DIALOG);
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Public navigation helpers (called by child Fragments)
    // -------------------------------------------------------------------------

    /**
     * Navigates to {@link HistoryFragment}.
     *
     * <p>Pushed on top of the back stack — Back press returns to caller.</p>
     */
    public void openHistoryFragment() {
        FileLogger.d(TAG, "Opening HistoryFragment.");
        openFragment(new HistoryFragment(), TAG_HISTORY);
    }

    /**
     * Navigates to {@link LanguageFragment} (language pack manager).
     *
     * <p>Pushed on top of the back stack — Back press returns to caller.</p>
     */
    public void openLanguageFragment() {
        FileLogger.d(TAG, "Opening LanguageFragment.");
        openFragment(new LanguageFragment(), TAG_LANGUAGE);
    }

    /**
     * Navigates to {@link CollectionFragment} (user collections).
     *
     * <p>Triggered by the toolbar collection icon. Pushed on back stack.</p>
     */
    public void openCollectionFragment() {
        FileLogger.d(TAG, "Opening CollectionFragment.");
        openFragment(new CollectionFragment(), TAG_COLLECTION);
    }
	
	public void openCollectionDetailFragment(long id, @NonNull String name, @NonNull String colorHex) {
        FileLogger.d(TAG, "Opening CollectionDetailFragment: " + name);
        openFragment(
            CollectionDetailFragment.newInstance(id, name, colorHex),
            TAG_COLLECTION_DETAIL
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads {@link TranslateFragment} as the non-back-stackable root fragment.
     *
     * <p>The root is never added to the back stack so that pressing Back
     * from it exits the app naturally.</p>
     */
    private void loadRootFragment() {
        FileLogger.d(TAG, "Loading root TranslateFragment.");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), new TranslateFragment(), TAG_TRANSLATE)
                .commit();
    }

    /**
     * Switches the active tab fragment without adding to the back stack.
     *
     * <p>Uses {@code show/hide} pattern to preserve each tab's state across
     * switches. If the fragment does not yet exist it is added fresh.</p>
     *
     * <p>Unlike {@link #openFragment(Fragment, String)} this does NOT push
     * to the back stack — Back from any tab exits the app.</p>
     *
     * @param fragment new tab fragment instance (used only on first visit)
     * @param tag      unique tag identifying this tab's fragment
     */
    private void switchTab(@NonNull Fragment fragment, @NonNull String tag) {
        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                    R.anim.fragment_enter,
                    R.anim.fragment_exit,
                    R.anim.fragment_pop_enter,
                    R.anim.fragment_pop_exit)
            .replace(binding.fragmentContainer.getId(), fragment, tag)
            .commit();
    }

    /**
     * Opens a secondary fragment by pushing it onto the back stack.
     *
     * <p>If an instance with the same {@code tag} already exists in the
     * back stack it is popped to the top to avoid duplicates. Otherwise
     * a fresh transaction with slide animations is committed.</p>
     *
     * @param fragment the {@link Fragment} instance to display
     * @param tag      unique back-stack tag identifying this fragment type
     */
    private void openFragment(@NonNull Fragment fragment, @NonNull String tag) {
        FragmentManager fm = getSupportFragmentManager();

        boolean popped = fm.popBackStackImmediate(tag, 0);
        if (popped) {
            FileLogger.d(TAG, "Popped to existing fragment: " + tag);
            return;
        }

        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.fragment_enter,
                R.anim.fragment_exit,
                R.anim.fragment_pop_enter,
                R.anim.fragment_pop_exit
        );
        transaction.replace(binding.fragmentContainer.getId(), fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commit();

        FileLogger.d(TAG, "Fragment transaction committed: " + tag);
    }

    /**
     * Determines whether the user is authenticated and routes accordingly.
     *
     * <p><b>Placeholder logic:</b> always routes to {@link LoginActivity}
     * until a real {@code UserSession} / auth layer is implemented.</p>
     *
     * <p>When auth is ready, replace the {@code isLoggedIn} check with the
     * actual session check, e.g.:</p>
     * <pre>
     *     boolean isLoggedIn = UserSession.getInstance(this).isLoggedIn();
     * </pre>
     */
    private void openProfileOrLogin() {
        // TODO: replace with real auth check
        boolean isLoggedIn = false;

        if (isLoggedIn) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
	
	/**
     * Switches the bottom navigation to the Favorite tab programmatically.
     * Called by child Fragments that need to open the favorite screen.
     */
	public void switchToFavoriteTab() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_favorite);
    }
	
	/**
     * Switches the bottom navigation to the Photo tab programmatically.
     * Called by child Fragments that need to open the photo/OCR screen.
     */
    public void switchToPhotoTab() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_photo);
    }
	
	/** Pending HistoryItem to restore into TranslateFragment after tab switch. */
    @Nullable
    private com.igoy86.nexttranslate.domain.model.HistoryItem pendingRestore = null;

    /**
     * Switches the bottom navigation to the Text (Translate) tab and
     * brings TranslateFragment to the foreground.
     * Called by FavoriteFragment when the user taps a favorite item
     * while on the Favorite tab.
     */
    public void switchToTextTab() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_text);
    }

    /**
     * Switches to the Text tab and schedules a restore into TranslateFragment
     * after the fragment transaction commits.
     *
     * @param item the HistoryItem to restore
     */
    public void switchToTextTabWithRestore(
        @NonNull com.igoy86.nexttranslate.domain.model.HistoryItem item) {
            pendingRestore = item;
            binding.bottomNavigation.setSelectedItemId(R.id.nav_text);
            // TranslateFragment akan di-replace via switchTab() → onStart/onResume
            // dipanggil setelah commit. Gunakan Handler agar fragment sudah attach.
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                final androidx.fragment.app.Fragment f =
                    getSupportFragmentManager().findFragmentByTag(TAG_TRANSLATE);
                    if (f instanceof TranslateFragment && pendingRestore != null) {
                        ((TranslateFragment) f).restoreFromHistory(pendingRestore);
                        pendingRestore = null;
                    }
            });
        }
}