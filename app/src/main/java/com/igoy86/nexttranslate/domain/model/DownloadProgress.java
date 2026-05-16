package com.igoy86.nexttranslate.domain.model;

import androidx.annotation.NonNull;

/**
 * Represents the progress of an ML Kit language model download.
 *
 * <p>Holds the language code being downloaded along with the number of
 * bytes downloaded so far and the total file size in bytes. Used to
 * populate a per-item progress indicator in the Language Manager screen.</p>
 */
public class DownloadProgress {

    /** BCP-47 language code of the model being downloaded (e.g. "id"). */
    @NonNull
    private final String languageCode;

    /** Number of bytes downloaded so far. */
    private final long bytesDownloaded;

    /** Total size of the model file in bytes. */
    private final long totalBytes;

    /**
     * Constructs a new {@link DownloadProgress}.
     *
     * @param languageCode    the BCP-47 code of the language being downloaded
     * @param bytesDownloaded bytes downloaded so far
     * @param totalBytes      total file size in bytes
     */
    public DownloadProgress(
            @NonNull String languageCode,
            long bytesDownloaded,
            long totalBytes
    ) {
        this.languageCode = languageCode;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytes = totalBytes;
    }

    /** Returns the BCP-47 language code of the model being downloaded. */
    @NonNull
    public String getLanguageCode() {
        return languageCode;
    }

    /** Returns the number of bytes downloaded so far. */
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    /** Returns the total size of the model file in bytes. */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Returns the download progress as a percentage (0–100).
     * Returns 0 if {@code totalBytes} is zero to avoid division by zero.
     */
    public int getPercent() {
        if (totalBytes <= 0) return 0;
        return (int) (bytesDownloaded * 100L / totalBytes);
    }
}
