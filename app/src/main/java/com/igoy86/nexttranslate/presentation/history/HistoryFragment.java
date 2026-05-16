package com.igoy86.nexttranslate.presentation.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.databinding.FragmentHistoryBinding;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Fragment for the translation history screen.
 *
 * <p>Displays all past translation entries in a RecyclerView ordered
 * from newest to oldest. Supports deleting individual entries and
 * clearing all history via a confirmation dialog.</p>
 *
 * <p>Observes {@link HistoryViewModel} for the history list and
 * error events.</p>
 *
 * <p>UI components handled in this Fragment:</p>
 * <ul>
 *     <li>History RecyclerView</li>
 *     <li>Empty state view (shown when history is empty)</li>
 *     <li>Clear all history button</li>
 * </ul>
 */
public class HistoryFragment extends BaseFragment<FragmentHistoryBinding> {

    /** Tag used for logging events originating from this Fragment. */
    private static final String TAG = "HistoryFragment";

    /** ViewModel managing the history UI state. */
    private HistoryViewModel viewModel;

    /** Adapter for the history RecyclerView. */
    private HistoryAdapter historyAdapter;

    // -------------------------------------------------------------------------
    // BaseFragment implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected FragmentHistoryBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentHistoryBinding.inflate(inflater, container, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes the {@link HistoryViewModel} and sets up the
     * {@link HistoryAdapter} with the RecyclerView.</p>
     */
    @Override
    protected void initViews() {
        final AppContainer container = NextTranslateApp.getContainer();

        final HistoryViewModelFactory factory = new HistoryViewModelFactory(
                container.getGetAllHistoryUseCase(),
                container.getDeleteHistoryUseCase(),
                container.getClearAllHistoryUseCase()
        );

        viewModel = new ViewModelProvider(this, factory).get(HistoryViewModel.class);

        // Setup RecyclerView
        historyAdapter = new HistoryAdapter(new HistoryAdapter.OnHistoryItemClickListener() {

            /**
             * Handles item tap — navigates back to translate screen
             * with the selected history item pre-filled.
             */
            @Override
            public void onItemClick(@NonNull HistoryItem item) {
                // TODO: Navigate to TranslateFragment with pre-filled text
                FileLogger.d(TAG, "History item clicked: " + item.getSourceText());
            }

            /**
             * Handles delete tap — removes the selected history entry.
             */
            @Override
            public void onDeleteClick(@NonNull HistoryItem item) {
                viewModel.deleteHistory(item.getId());
                FileLogger.d(TAG, "Delete history requested: id=" + item.getId());
            }
        });

        getBinding().recyclerViewHistory.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        getBinding().recyclerViewHistory.setAdapter(historyAdapter);

        FileLogger.d(TAG, "HistoryFragment views initialized.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Observes the history list from {@link HistoryViewModel} and
     * updates the RecyclerView adapter and empty state visibility.</p>
     */
    @Override
    protected void initObservers() {
        // Observe history list
        viewModel.getHistoryListLiveData().observe(getViewLifecycleOwner(), historyItems -> {
            historyAdapter.submitList(historyItems);

            // Toggle empty state visibility
            if (historyItems == null || historyItems.isEmpty()) {
                getBinding().layoutEmptyState.setVisibility(android.view.View.VISIBLE);
                getBinding().recyclerViewHistory.setVisibility(android.view.View.GONE);
                FileLogger.d(TAG, "History list is empty.");
            } else {
                getBinding().layoutEmptyState.setVisibility(android.view.View.GONE);
                getBinding().recyclerViewHistory.setVisibility(android.view.View.VISIBLE);
                FileLogger.d(TAG, "History list updated. Count: " + historyItems.size());
            }
        });

        // Observe error messages
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Attaches a click listener to the clear all history button
     * that shows a confirmation dialog before proceeding.</p>
     */
    @Override
    protected void initListeners() {
        getBinding().buttonClearHistory.setOnClickListener(v -> showClearHistoryConfirmDialog());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Shows a confirmation dialog before clearing all history entries.
     *
     * <p>Only proceeds with clearing if the user explicitly confirms
     * the action.</p>
     */
    private void showClearHistoryConfirmDialog() {
        if (getContext() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Clear History")
                .setMessage("Are you sure you want to delete all translation history? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    viewModel.clearAllHistory();
                    FileLogger.d(TAG, "Clear all history confirmed by user.");
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}