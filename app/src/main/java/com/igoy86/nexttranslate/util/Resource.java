package com.igoy86.nexttranslate.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A generic wrapper class that represents a loading state associated with its data.
 *
 * <p>Used to expose network/database results back to the UI layer while keeping
 * the data and its state (loading, success, error, progress) together in a single
 * object. Typically observed via {@link androidx.lifecycle.LiveData}.</p>
 *
 * @param <T> the type of data held by this resource
 */
public class Resource<T> {

    /**
     * Represents the possible states of a {@link Resource}.
     */
    public enum Status {
        /** The operation completed successfully. */
        SUCCESS,
        /** The operation encountered an error. */
        ERROR,
        /** The operation is currently in progress (indeterminate). */
        LOADING,
        /**
         * The operation is downloading with known progress.
         * {@link Resource#getData()} contains a progress payload (e.g. {@link com.igoy86.nexttranslate.domain.model.DownloadProgress}).
         */
        PROGRESS
    }

    /** The current status of this resource. */
    @NonNull
    private final Status status;

    /**
     * The data associated with this resource.
     * May be {@code null} during loading or on error.
     */
    @Nullable
    private final T data;

    /**
     * A human-readable error message.
     * Only present when {@link #status} is {@link Status#ERROR}.
     */
    @Nullable
    private final String message;

    /**
     * Private constructor to enforce creation via static factory methods.
     *
     * @param status  the current status of this resource
     * @param data    the associated data, or {@code null}
     * @param message the error message, or {@code null}
     */
    private Resource(
            @NonNull Status status,
            @Nullable T data,
            @Nullable String message
    ) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link Resource} in the {@link Status#SUCCESS} state.
     *
     * @param data the successfully retrieved data
     * @param <T>  the type of data
     * @return a {@link Resource} wrapping the given data with SUCCESS status
     */
    @NonNull
    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    /**
     * Creates a {@link Resource} in the {@link Status#ERROR} state.
     *
     * @param message a human-readable description of the error
     * @param data    optional stale data to display alongside the error
     * @param <T>     the type of data
     * @return a {@link Resource} with ERROR status and the given message
     */
    @NonNull
    public static <T> Resource<T> error(
            @NonNull String message,
            @Nullable T data
    ) {
        return new Resource<>(Status.ERROR, data, message);
    }

    /**
     * Creates a {@link Resource} in the {@link Status#LOADING} state.
     *
     * @param data optional partial data available during loading
     * @param <T>  the type of data
     * @return a {@link Resource} with LOADING status
     */
    @NonNull
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    /**
     * Creates a {@link Resource} in the {@link Status#PROGRESS} state.
     *
     * <p>Use this when the operation has deterministic progress to report,
     * for example during an ML Kit model download. Pass a
     * {@link com.igoy86.nexttranslate.domain.model.DownloadProgress} as {@code data}.</p>
     *
     * @param data the progress payload; should not be {@code null}
     * @param <T>  the type of data (typically {@link com.igoy86.nexttranslate.domain.model.DownloadProgress})
     * @return a {@link Resource} with PROGRESS status
     */
    @NonNull
    public static <T> Resource<T> progress(@Nullable T data) {
        return new Resource<>(Status.PROGRESS, data, null);
    }

    // -------------------------------------------------------------------------
    // Convenience check methods
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this resource is in the {@link Status#SUCCESS} state.
     *
     * @return {@code true} if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Returns {@code true} if this resource is in the {@link Status#ERROR} state.
     *
     * @return {@code true} if status is ERROR
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * Returns {@code true} if this resource is in the {@link Status#LOADING} state.
     *
     * @return {@code true} if status is LOADING
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }

    /**
     * Returns {@code true} if this resource is in the {@link Status#PROGRESS} state.
     *
     * @return {@code true} if status is PROGRESS
     */
    public boolean isProgress() {
        return status == Status.PROGRESS;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the current status of this resource.
     *
     * @return the {@link Status} of this resource
     */
    @NonNull
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the data associated with this resource.
     *
     * @return the data, or {@code null} if not available
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Returns the error message associated with this resource.
     *
     * @return the error message, or {@code null} if not an error state
     */
    @Nullable
    public String getMessage() {
        return message;
    }
}
