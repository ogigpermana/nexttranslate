package com.igoy86.nexttranslate.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * UserSession — Singleton that persists Google SSO login state.
 *
 * <p>Backed by {@link SharedPreferences}. Stores display name, email,
 * and photo URL from the Google ID token after a successful sign-in.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *     UserSession session = UserSession.getInstance(context);
 *     if (session.isLoggedIn()) {
 *         String name = session.getDisplayName();
 *     }
 * </pre>
 */
public final class UserSession {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String PREFS_NAME   = "user_session";
    private static final String KEY_NAME     = "display_name";
    private static final String KEY_EMAIL    = "email";
    private static final String KEY_PHOTO    = "photo_url";
    private static final String KEY_LOGGED_IN = "is_logged_in";
	private static final String KEY_THEME = "theme_mode";
	private static final String KEY_OFFLINE_MODE = "offline_mode";

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    @Nullable
    private static volatile UserSession instance;

    @NonNull
    public static UserSession getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final SharedPreferences prefs;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private UserSession(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Saves the authenticated user's profile data.
     *
     * @param displayName user's full name from Google profile
     * @param email       user's Google email address
     * @param photoUrl    URL of the user's Google profile photo (may be null)
     */
    public void saveUser(
            @NonNull String displayName,
            @NonNull String email,
            @Nullable String photoUrl) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_NAME, displayName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHOTO, photoUrl != null ? photoUrl : "")
                .apply();
    }

    /**
     * Clears all session data — called on logout.
     */
    public void clear() {
        prefs.edit().clear().apply();
    }
	
	/**
     * Save theme mode
     */
	public void saveThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }
	
	/**
     * Save offline/online mode
     */
	public void saveOfflineMode(boolean isOffline) {
        prefs.edit().putBoolean(KEY_OFFLINE_MODE, isOffline).apply();
    }

    public boolean isOfflineMode() {
        return prefs.getBoolean(KEY_OFFLINE_MODE, false);
    }

    public int getThemeMode() {
        // Default: MODE_NIGHT_FOLLOW_SYSTEM (-1)
        return prefs.getInt(KEY_THEME,
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /** Returns {@code true} if the user is currently signed in. */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    /** Returns the user's display name, or {@code "Guest User"} if not set. */
    @NonNull
    public String getDisplayName() {
        return prefs.getString(KEY_NAME, "Guest User");
    }

    /** Returns the user's email address, or empty string if not set. */
    @NonNull
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    /**
     * Returns the user's Google profile photo URL,
     * or {@code null} if not available.
     */
    @Nullable
    public String getPhotoUrl() {
        final String url = prefs.getString(KEY_PHOTO, "");
        return (url != null && !url.isEmpty()) ? url : null;
    }
}
