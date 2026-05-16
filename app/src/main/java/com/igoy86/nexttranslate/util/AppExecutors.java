package com.igoy86.nexttranslate.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Global executor pools for the whole application.
 *
 * <p>Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait
 * behind application code). It also provides a single point of control for thread management,
 * making it easier to swap out implementations during testing.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     AppExecutors executors = AppExecutors.getInstance();
 *     executors.diskIO().execute(() -> {
 *         // perform database operation
 *     });
 *     executors.mainThread().execute(() -> {
 *         // update UI
 *     });
 * </pre>
 */
public class AppExecutors {

    /** Number of threads allocated for network-related operations. */
    private static final int NETWORK_THREAD_COUNT = 3;

    /** Singleton instance of AppExecutors. */
    private static volatile AppExecutors instance;

    /** Executor for disk I/O operations such as database reads and writes. */
    private final Executor diskIO;

    /** Executor for network operations such as API calls and model downloads. */
    private final Executor networkIO;

    /** Executor for running tasks on the Android main (UI) thread. */
    private final Executor mainThread;

    /**
     * Private constructor to enforce singleton pattern.
     *
     * @param diskIO     executor for disk operations
     * @param networkIO  executor for network operations
     * @param mainThread executor for main thread operations
     */
    private AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    /**
     * Returns the singleton instance of {@link AppExecutors}.
     *
     * <p>Uses double-checked locking to ensure thread-safe lazy initialization.</p>
     *
     * @return the singleton {@link AppExecutors} instance
     */
    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors(
                            Executors.newSingleThreadExecutor(),
                            Executors.newFixedThreadPool(NETWORK_THREAD_COUNT),
                            new MainThreadExecutor()
                    );
                }
            }
        }
        return instance;
    }

    /**
     * Returns the executor for disk I/O operations.
     *
     * <p>Use this executor for Room database reads/writes and file operations
     * to avoid blocking the main thread.</p>
     *
     * @return single-thread {@link Executor} for disk operations
     */
    public Executor diskIO() {
        return diskIO;
    }

    /**
     * Returns the executor for network I/O operations.
     *
     * <p>Use this executor for ML Kit model downloads and any
     * network-related background tasks.</p>
     *
     * @return fixed-thread-pool {@link Executor} for network operations
     */
    public Executor networkIO() {
        return networkIO;
    }

    /**
     * Returns the executor that posts tasks to the Android main (UI) thread.
     *
     * <p>Use this executor to update UI components after completing
     * background work.</p>
     *
     * @return main-thread {@link Executor}
     */
    public Executor mainThread() {
        return mainThread;
    }

    /**
     * An {@link Executor} that posts tasks to the Android main (UI) thread
     * using a {@link Handler} backed by the main {@link Looper}.
     */
    private static class MainThreadExecutor implements Executor {

        /** Handler attached to the main looper for posting UI-thread tasks. */
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        /**
         * Posts the given {@code command} to run on the main thread.
         *
         * @param command the runnable task to execute on the main thread
         */
        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}