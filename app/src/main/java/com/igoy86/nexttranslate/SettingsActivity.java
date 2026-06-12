package com.igoy86.nexttranslate;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.igoy86.nexttranslate.databinding.ActivitySettingsBinding;
import com.igoy86.nexttranslate.presentation.base.BaseActivity;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.UserSession;

/**
 * SettingsActivity — Application settings and user profile screen.
 *
 * <p>Displays the authenticated user's profile at the top (avatar, name,
 * email) followed by grouped setting rows. If the user is a guest, shows
 * placeholder text and hides the logout button.</p>
 */
public class SettingsActivity extends BaseActivity<ActivitySettingsBinding> {

    private static final String TAG = "SettingsActivity";

    private UserSession userSession;

    // -------------------------------------------------------------------------
    // BaseActivity contract
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    protected ActivitySettingsBinding initBinding() {
        return ActivitySettingsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initViews() {
        userSession = UserSession.getInstance(this);

        // Toolbar back navigation
        // Apply status bar inset to root
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
        binding.rootSettings, (v, insets) -> {
            final int top = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        binding.toolbarSettings.setNavigationOnClickListener(v -> {
            FileLogger.d(TAG, "Back pressed from SettingsActivity.");
            finish();
        });


        loadUserProfile();
		
		// Set active theme button based on current night mode
        final int currentMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
            binding.btnThemeLight.setChecked(true);
        } else if (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
            binding.btnThemeDark.setChecked(true);
        } else {
            binding.btnThemeSystem.setChecked(true);
        }
    }

    @Override
    protected void initObservers() {
        // No LiveData — state driven by UserSession (SharedPreferences)
    }

    @Override
    protected void initListeners() {

        // --- Avatar tap — open LoginActivity if guest ---
        binding.ivAvatar.setOnClickListener(v -> {
            if (!userSession.isLoggedIn()) {
                FileLogger.d(TAG, "Guest tapped avatar — routing to LoginActivity.");
                startActivity(new Intent(this, LoginActivity.class));
            }
        });

        // --- Theme toggle ---
        binding.btnThemeLight.setOnClickListener(v -> {
            final int mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
            userSession.saveThemeMode(mode);
            FileLogger.d(TAG, "Theme: Light selected.");
        });
        binding.btnThemeDark.setOnClickListener(v -> {
            final int mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
            userSession.saveThemeMode(mode);
            FileLogger.d(TAG, "Theme: Dark selected.");
        });
        binding.btnThemeSystem.setOnClickListener(v -> {
            final int mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
            userSession.saveThemeMode(mode);
            FileLogger.d(TAG, "Theme: System selected.");
        });

        // --- Translation history toggle ---
        binding.switchHistory.setOnCheckedChangeListener((btn, isChecked) ->
                FileLogger.d(TAG, "History toggle: " + isChecked));

        // --- Offline mode toggle ---
        binding.switchOffline.setOnCheckedChangeListener((btn, isChecked) -> {
            userSession.saveOfflineMode(isChecked);
            FileLogger.d(TAG, "Offline mode saved: " + isChecked);
        });

        // --- Download language packs ---
        binding.rowLanguagePacks.setOnClickListener(v -> {
            FileLogger.d(TAG, "Language packs tapped.");
            startActivity(new Intent(this, LanguageActivity.class));
        });

        // --- Auto-detect language toggle ---
        binding.switchAutoDetect.setOnCheckedChangeListener((btn, isChecked) ->
                FileLogger.d(TAG, "Auto-detect toggle: " + isChecked));

        // --- Rate on Play Store ---
        binding.rowRate.setOnClickListener(v -> {
            FileLogger.d(TAG, "Rate on Play Store tapped.");
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + getPackageName())));
            } catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id="
                                + getPackageName())));
            }
        });

        // --- Send feedback ---
        binding.rowFeedback.setOnClickListener(v -> {
            FileLogger.d(TAG, "Feedback tapped.");
            final Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:ogigpermana@gmail.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, "NextTranslate Feedback");
            startActivity(Intent.createChooser(email, "Send feedback"));
        });

        // --- Help / FAQ ---
        binding.rowHelp.setOnClickListener(v -> {
            FileLogger.d(TAG, "Help tapped.");
            showSnackbar("Help & FAQ coming soon.");
        });

        // --- Logout ---
        binding.btnLogout.setOnClickListener(v -> {
            FileLogger.d(TAG, "Logout tapped.");
            userSession.clear();
            showSnackbar(getString(R.string.logout_success));
            final Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile in case user just logged in from avatar tap
        loadUserProfile();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads the user's profile data into the header views.
     * Shows guest placeholders when not logged in.
     */
    private void loadUserProfile() {
        FileLogger.d(TAG, "isLoggedIn: " + userSession.isLoggedIn()
                + " | name: " + userSession.getDisplayName()
                + " | email: " + userSession.getEmail());

        if (userSession.isLoggedIn()) {
            binding.tvDisplayName.setText(userSession.getDisplayName());
            binding.tvEmail.setText(userSession.getEmail());
            binding.btnLogout.setVisibility(android.view.View.VISIBLE);

            // Load profile photo via Glide
            final String photoUrl = userSession.getPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivAvatar);
            }

            FileLogger.d(TAG, "Profile loaded: " + userSession.getEmail());
        } else {
            // Guest state
            binding.tvDisplayName.setText(R.string.settings_guest_name);
            binding.tvEmail.setText(R.string.settings_guest_email);
            binding.btnLogout.setVisibility(android.view.View.GONE);
            binding.ivAvatar.setImageResource(R.drawable.ic_person);
            FileLogger.d(TAG, "Guest profile displayed.");
        }
		
		// Restore offline mode switch state
        binding.switchOffline.setChecked(userSession.isOfflineMode());
    }
}
