package com.igoy86.nexttranslate;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.igoy86.nexttranslate.databinding.ActivityLoginBinding;
import com.igoy86.nexttranslate.presentation.base.BaseActivity;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.util.UserSession;

import java.util.concurrent.Executors;

/**
 * LoginActivity — Authentication entry point for NextTranslate.
 *
 * <p>Supports:</p>
 * <ul>
 *     <li>Google Sign-In via Credential Manager (no Firebase)</li>
 *     <li>Continue as Guest (skips auth)</li>
 * </ul>
 *
 * <p>On successful Google Sign-In, saves user profile to {@link UserSession}
 * then navigates to {@link SettingsActivity}.</p>
 */
public class LoginActivity extends BaseActivity<ActivityLoginBinding> {

    private static final String TAG = "LoginActivity";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private CredentialManager credentialManager;
    private UserSession userSession;

    // -------------------------------------------------------------------------
    // BaseActivity contract
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    protected ActivityLoginBinding initBinding() {
        return ActivityLoginBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initViews() {
        credentialManager = CredentialManager.create(this);
        userSession = UserSession.getInstance(this);

        // If already logged in, skip to Settings
        if (userSession.isLoggedIn()) {
            FileLogger.d(TAG, "Already logged in, routing to SettingsActivity.");
            goToSettings();
            return;
        }
    }

    @Override
    protected void initObservers() {
        // No LiveData — auth result handled via Credential Manager callback
    }

    @Override
    protected void initListeners() {
        // --- Google Sign-In ---
        binding.btnGoogle.setOnClickListener(v -> {
            FileLogger.d(TAG, "Google Sign-In tapped.");
            launchGoogleSignIn();
        });

        // --- Continue as guest ---
        binding.btnGuest.setOnClickListener(v -> {
            FileLogger.d(TAG, "Continue as guest tapped.");
            finish();
        });
    }

    // -------------------------------------------------------------------------
    // Google Sign-In
    // -------------------------------------------------------------------------

    /**
     * Launches the Google Sign-In bottom sheet via Credential Manager.
     *
     * <p>Uses {@link GetGoogleIdOption} with {@code filterByAuthorizedAccounts = false}
     * so both previously authorized and new accounts are shown.</p>
     */
    private void launchGoogleSignIn() {
        final GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        final GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> handleSignInResult(result));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> {
							FileLogger.w(TAG, "Google Sign-In error type: " + e.getClass().getSimpleName());
                            FileLogger.w(TAG, "Google Sign-In error: " + e.getMessage());
                            final String msg = e.getMessage() != null
                                    && e.getMessage().contains("cancel")
                                    ? getString(R.string.login_error_cancelled)
                                    : getString(R.string.login_error_google);
                            showSnackbar(msg);
                        });
                    }
                }
        );
    }

    /**
     * Processes the credential returned by Credential Manager.
     *
     * <p>Extracts display name, email and photo URL from the
     * {@link GoogleIdTokenCredential} and persists them via
     * {@link UserSession}.</p>
     *
     * @param result the successful credential response
     */
    private void handleSignInResult(@NonNull GetCredentialResponse result) {
        final Credential credential = result.getCredential();

        try {
            final GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(credential.getData());

            final String name     = googleCredential.getDisplayName() != null
                    ? googleCredential.getDisplayName() : "User";
            final String email    = googleCredential.getId();
            final String photoUrl = googleCredential.getProfilePictureUri() != null
                    ? googleCredential.getProfilePictureUri().toString() : null;

            userSession.saveUser(name, email, photoUrl);

            FileLogger.d(TAG, "Google Sign-In success: " + email);
            showSnackbar(getString(R.string.login_success));
            goToSettings();

        } catch (Exception e) {
            FileLogger.w(TAG, "Failed to parse credential: " + e.getMessage());
            showSnackbar(getString(R.string.login_error_google));
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Navigates to {@link SettingsActivity} and clears this activity
     * from the back stack.
     */
    private void goToSettings() {
        final Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
