package com.igoy86.nexttranslate.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

/**
 * Utility class for checking network connectivity status.
 *
 * <p>Used as a guard before allowing the user to switch to Online
 * (Groq) translation mode, and as a fallback trigger when an online
 * translation request fails due to connectivity loss.</p>
 *
 * <p>All methods are static and stateless — no instantiation required.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     if (NetworkUtils.isInternetAvailable(requireContext())) {
 *         // proceed with online mode
 *     } else {
 *         // show Snackbar and stay in offline mode
 *     }
 * </pre>
 *
 * <p>Requires permission in {@code AndroidManifest.xml}:</p>
 * <pre>
 *     &lt;uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /&gt;
 * </pre>
 */
public final class NetworkUtils {

    /** Tag used for logging events originating from this utility. */
    private static final String TAG = "NetworkUtils";

    /**
     * Private constructor — this is a static utility class
     * and should never be instantiated.
     */
    private NetworkUtils() {
        throw new UnsupportedOperationException("NetworkUtils is a static utility class.");
    }

    /**
     * Checks whether the device currently has an active internet connection.
     *
     * <p>Uses {@link NetworkCapabilities} (API 23+) to verify that the
     * active network has actual internet capability, not just that a network
     * interface is connected. This correctly handles cases where Wi-Fi is
     * connected but has no internet access (captive portals, etc.).</p>
     *
     * <p>This is a synchronous, lightweight check suitable for calling on
     * the main thread before triggering a mode switch.</p>
     *
     * @param context any valid {@link Context}; must not be null
     * @return {@code true} if the device has an active internet connection,
     *         {@code false} otherwise (no network, airplane mode, no capability)
     */
    public static boolean isInternetAvailable(@NonNull Context context) {
        final ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            FileLogger.w(TAG, "ConnectivityManager is null — assuming no internet.");
            return false;
        }

        final Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            FileLogger.d(TAG, "No active network.");
            return false;
        }

        final NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            FileLogger.d(TAG, "NetworkCapabilities is null.");
            return false;
        }

        final boolean hasInternet = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        FileLogger.d(TAG, "Internet available: " + hasInternet);
        return hasInternet;
    }
}
