package com.igoy86.nexttranslate.presentation.history;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.R;
import com.igoy86.nexttranslate.databinding.FragmentHistoryBinding;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Fragment for the translation history screen.
 *
 * <p>Displays all past translation entries in a RecyclerView ordered
 * from newest to oldest — Gmail inbox style: items share one rounded
 * surface container with a thin divider between rows.</p>
 *
 * <p>Supported interactions:</p>
 * <ul>
 *     <li>Tap item    — restores translation to TranslateFragment</li>
 *     <li>Swipe left/right — deletes item with Snackbar + Undo</li>
 *     <li>Toolbar clear-all icon — confirms then clears entire history</li>
 * </ul>
 *
 * <p>Window insets: root LinearLayout applies status-bar top padding via
 * {@link androidx.core.view.ViewCompat#setOnApplyWindowInsetsListener} so
 * the toolbar is never overlapped by the status bar on any API level.</p>
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

    /** {@inheritDoc} */
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
     * <p>Sets up window insets, initialises {@link HistoryViewModel},
     * configures the {@link HistoryAdapter} and RecyclerView, and attaches
     * the swipe-to-delete {@link ItemTouchHelper}.</p>
     */
    @Override
    protected void initViews() {
        // Apply status bar top inset to root so toolbar clears the status bar.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                getBinding().rootHistory, (v, insets) -> {
                    final int statusBarHeight = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                    v.setPadding(v.getPaddingLeft(), statusBarHeight,
                            v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        final AppContainer container = NextTranslateApp.getContainer();
        final HistoryViewModelFactory factory = new HistoryViewModelFactory(
                container.getGetAllHistoryUseCase(),
                container.getDeleteHistoryUseCase(),
                container.getClearAllHistoryUseCase(),
				container.getRestoreHistoryUseCase()
        );
        viewModel = new ViewModelProvider(this, factory).get(HistoryViewModel.class);

        // Adapter — tap only; delete is handled by swipe below
        historyAdapter = new HistoryAdapter(item -> {
            FileLogger.d(TAG, "History item tapped: " + item.getSourceText());
            if (getActivity() == null) return;

            getActivity().getSupportFragmentManager().popBackStack();
            getActivity().getSupportFragmentManager().executePendingTransactions();

            final androidx.fragment.app.Fragment translateFrag =
                    getActivity().getSupportFragmentManager()
                            .findFragmentByTag(com.igoy86.nexttranslate.MainActivity.TAG_TRANSLATE);

            if (translateFrag instanceof
                    com.igoy86.nexttranslate.presentation.translate.TranslateFragment) {
                ((com.igoy86.nexttranslate.presentation.translate.TranslateFragment) translateFrag)
                        .restoreFromHistory(item);
            }
        });

        // RecyclerView setup
        getBinding().recyclerViewHistory.setLayoutManager(
                new LinearLayoutManager(getContext()));
        getBinding().recyclerViewHistory.setAdapter(historyAdapter);

        // Attach swipe-to-delete
        buildItemTouchHelper().attachToRecyclerView(getBinding().recyclerViewHistory);

        FileLogger.d(TAG, "HistoryFragment views initialized.");
    }

    /** {@inheritDoc} */
    @Override
    protected void initObservers() {
        // History list
        viewModel.getHistoryListLiveData().observe(getViewLifecycleOwner(), historyItems -> {
            historyAdapter.submitList(historyItems == null
                    ? null : new java.util.ArrayList<>(historyItems));

            final boolean isEmpty = historyItems == null || historyItems.isEmpty();
            getBinding().layoutEmptyState.setVisibility(
                    isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
            getBinding().recyclerViewHistory.setVisibility(
                    isEmpty ? android.view.View.GONE : android.view.View.VISIBLE);

            FileLogger.d(TAG, isEmpty
                    ? "History empty."
                    : "History loaded: " + historyItems.size() + " items");
        });

        // Error messages
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
            }
        });

        // One-shot Snackbar (clear-all success)
        viewModel.getSnackbarMessageLiveData().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showSnackbar(message);
                viewModel.clearSnackbarMessage();
                FileLogger.d(TAG, "Snackbar shown: " + message);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected void initListeners() {
        // Back arrow
        getBinding().toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Toolbar menu — clear all history
        getBinding().toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_all_history) {
                showClearHistoryConfirmDialog();
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Swipe-to-delete
    // -------------------------------------------------------------------------

    /**
     * Builds and returns an {@link ItemTouchHelper} configured for
     * swipe-left and swipe-right to delete, with a red background and
     * delete icon drawn behind the swiping item.
     *
     * <p>After a successful swipe:</p>
     * <ol>
     *     <li>The item is deleted from the database via the ViewModel.</li>
     *     <li>A Snackbar is shown with an <b>Undo</b> action.</li>
     *     <li>If Undo is tapped within the timeout, the item is re-inserted.</li>
     * </ol>
     *
     * @return a configured {@link ItemTouchHelper} ready to attach to the RecyclerView
     */
    @NonNull
    private ItemTouchHelper buildItemTouchHelper() {
        return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, // no drag
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {

            /** Red background drawn behind the swiping row. */
            private final ColorDrawable swipeBg = new ColorDrawable(
                    0xFFB00020 // Material error red
            );

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                // Drag-and-drop not used.
                return false;
            }

            /**
             * Called when an item has been fully swiped off screen.
             * Captures the deleted item, removes it from the database,
             * then shows a Snackbar with an Undo action.
             *
             * @param viewHolder the swiped ViewHolder
             * @param direction  {@link ItemTouchHelper#LEFT} or {@link ItemTouchHelper#RIGHT}
             */
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_ID) return;

                final HistoryItem deletedItem = historyAdapter.getItemAt(position);
                FileLogger.d(TAG, "Swiped to delete id=" + deletedItem.getId());

                // Delete from DB
                viewModel.deleteHistory(deletedItem.getId());

                // Snackbar with Undo
                Snackbar.make(
                                getBinding().recyclerViewHistory,
                                R.string.snackbar_history_deleted,
                                Snackbar.LENGTH_LONG
                        )
                        .setAction(R.string.snackbar_undo, v -> {
                            // Re-insert the deleted item
                            viewModel.restoreHistory(deletedItem);
                            FileLogger.d(TAG, "Undo delete: id=" + deletedItem.getId());
                        })
                        .show();
            }

            /**
             * Draws a red background and a delete icon behind the swiping item
             * so the user has clear visual feedback about the delete action.
             *
             * @param c                 the Canvas to draw on
             * @param recyclerView      the RecyclerView
             * @param viewHolder        the swiping ViewHolder
             * @param dX                horizontal displacement (positive = right, negative = left)
             * @param dY                vertical displacement (unused)
             * @param actionState       current action state
             * @param isCurrentlyActive {@code true} while the user is actively dragging
             */
            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive);

                final android.view.View itemView = viewHolder.itemView;
                final int iconMargin = (itemView.getHeight()
                        - dpToPx(24)) / 2;

                // Draw red background
                if (dX > 0) {
                    // Swiping right
                    swipeBg.setBounds(
                            itemView.getLeft(),
                            itemView.getTop(),
                            itemView.getLeft() + (int) dX,
                            itemView.getBottom()
                    );
                } else if (dX < 0) {
                    // Swiping left
                    swipeBg.setBounds(
                            itemView.getRight() + (int) dX,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );
                } else {
                    swipeBg.setBounds(0, 0, 0, 0);
                }
                swipeBg.draw(c);

                // Draw delete icon
                final android.graphics.drawable.Drawable deleteIcon =
                        ContextCompat.getDrawable(requireContext(),
                                R.drawable.ic_delete_outline);
                if (deleteIcon == null) return;
                deleteIcon.setTint(0xFFFFFFFF); // white

                final int iconTop = itemView.getTop() + iconMargin;
                final int iconBottom = iconTop + dpToPx(24);

                if (dX > 0) {
                    // Icon on the left side when swiping right
                    deleteIcon.setBounds(
                            itemView.getLeft() + iconMargin,
                            iconTop,
                            itemView.getLeft() + iconMargin + dpToPx(24),
                            iconBottom
                    );
                } else if (dX < 0) {
                    // Icon on the right side when swiping left
                    deleteIcon.setBounds(
                            itemView.getRight() - iconMargin - dpToPx(24),
                            iconTop,
                            itemView.getRight() - iconMargin,
                            iconBottom
                    );
                }
                deleteIcon.draw(c);
            }

            /**
             * Converts dp to px using the current display density.
             *
             * @param dp value in density-independent pixels
             * @return equivalent value in screen pixels
             */
            private int dpToPx(int dp) {
                return (int) (dp * getResources().getDisplayMetrics().density);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Shows a confirmation dialog before clearing the entire history.
     * Proceeds only if the user confirms by tapping the positive button.
     */
    private void showClearHistoryConfirmDialog() {
        if (getContext() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.dialog_clear_history_title))
                .setMessage(getString(R.string.dialog_clear_history_message))
                .setPositiveButton(getString(R.string.dialog_clear_history_confirm),
                        (dialog, which) -> {
                            viewModel.clearAllHistory();
                            FileLogger.d(TAG, "Clear all history confirmed by user.");
                        })
                .setNegativeButton(getString(R.string.dialog_cancel),
                        (dialog, which) -> dialog.dismiss())
                .show();
    }
}