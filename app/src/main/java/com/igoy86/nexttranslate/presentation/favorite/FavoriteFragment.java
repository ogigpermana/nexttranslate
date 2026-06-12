package com.igoy86.nexttranslate.presentation.favorite;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.R;
import com.igoy86.nexttranslate.databinding.FragmentFavoriteBinding;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;
import com.igoy86.nexttranslate.MainActivity;

/**
 * Fragment for the favorite translations screen.
 *
 * <p>Displays all bookmarked translation entries — Gmail inbox style,
 * matching HistoryFragment design.</p>
 *
 * <p>Supported interactions:</p>
 * <ul>
 *     <li>Tap item         — restores translation to TranslateFragment</li>
 *     <li>Swipe left/right — deletes item with Snackbar + Undo</li>
 *     <li>Toolbar clear-all icon — confirms then clears all favorites</li>
 * </ul>
 *
 * <p>Window insets: root LinearLayout applies status-bar top padding so
 * the toolbar never overlaps the status bar.</p>
 */
public class FavoriteFragment extends BaseFragment<FragmentFavoriteBinding> {

    private static final String TAG = "FavoriteFragment";

    private FavoriteViewModel viewModel;
    private FavoriteAdapter favoriteAdapter;

    // -------------------------------------------------------------------------
    // BaseFragment
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    protected FragmentFavoriteBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentFavoriteBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initViews() {
        // Apply status bar top inset so toolbar clears the status bar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                getBinding().rootFavorite, (v, insets) -> {
                    final int statusBarHeight = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
                    v.setPadding(v.getPaddingLeft(), statusBarHeight,
                            v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        final AppContainer container = NextTranslateApp.getContainer();
        final FavoriteViewModelFactory factory = new FavoriteViewModelFactory(
                container.getGetAllFavoritesUseCase(),
                container.getDeleteFavoriteUseCase(),
                container.getRestoreFavoriteUseCase(),
                container.getClearAllFavoritesUseCase()
        );
        viewModel = new ViewModelProvider(this, factory).get(FavoriteViewModel.class);

        // Adapter — tap only; delete via swipe
         favoriteAdapter = new FavoriteAdapter(item -> {
            FileLogger.d(TAG, "Favorite item tapped: " + item.getSourceText());
            if (getActivity() == null) return;

            final com.igoy86.nexttranslate.domain.model.HistoryItem historyItem =
                    new com.igoy86.nexttranslate.domain.model.HistoryItem(
                            item.getId(),
                            item.getSourceText(),
                            item.getTranslatedText(),
                            item.getSourceLanguageCode(),
                            item.getTargetLanguageCode(),
                            item.getSavedAt()
                    );

            ((com.igoy86.nexttranslate.MainActivity) getActivity())
                    .switchToTextTabWithRestore(historyItem);
        });

        getBinding().recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        getBinding().recyclerViewFavorites.setAdapter(favoriteAdapter);
        buildItemTouchHelper().attachToRecyclerView(getBinding().recyclerViewFavorites);
        
		getBinding().toolbar.setNavigationIcon(null);
        FileLogger.d(TAG, "FavoriteFragment views initialized.");
    }

    @Override
    protected void initObservers() {
        // Favorites list
        viewModel.getFavoritesListLiveData().observe(getViewLifecycleOwner(), favoriteItems -> {
            favoriteAdapter.submitList(favoriteItems == null
                    ? null : new java.util.ArrayList<>(favoriteItems));

            final boolean isEmpty = favoriteItems == null || favoriteItems.isEmpty();
            getBinding().layoutEmptyState.setVisibility(
                    isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
            getBinding().recyclerViewFavorites.setVisibility(
                    isEmpty ? android.view.View.GONE : android.view.View.VISIBLE);
        });

        // Snackbar (clear-all success)
        viewModel.getSnackbarMessageLiveData().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showSnackbar(message);
                viewModel.clearSnackbarMessage();
            }
        });
    }

    @Override
    protected void initListeners() {
        // Toolbar menu — clear all
        getBinding().toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_all_favorites) {
                showClearFavoritesConfirmDialog();
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Swipe-to-delete
    // -------------------------------------------------------------------------

    /**
     * Builds an {@link ItemTouchHelper} for swipe-left/right delete with
     * red background, delete icon, Snackbar + Undo.
     */
    @NonNull
    private ItemTouchHelper buildItemTouchHelper() {
        return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            private final ColorDrawable swipeBg = new ColorDrawable(0xFFB00020);

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_ID) return;

                final FavoriteItem deletedItem = favoriteAdapter.getItemAt(position);
                viewModel.deleteFavorite(deletedItem.getId());

                Snackbar.make(
                                getBinding().recyclerViewFavorites,
                                R.string.snackbar_favorite_deleted,
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_undo, v -> {
                            viewModel.restoreFavorite(deletedItem);
                            FileLogger.d(TAG, "Undo delete favorite: id=" + deletedItem.getId());
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive);

                final android.view.View itemView = viewHolder.itemView;
                final int iconMargin = (itemView.getHeight() - dpToPx(24)) / 2;

                if (dX > 0) {
                    swipeBg.setBounds(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + (int) dX, itemView.getBottom());
                } else if (dX < 0) {
                    swipeBg.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                } else {
                    swipeBg.setBounds(0, 0, 0, 0);
                }
                swipeBg.draw(c);

                final android.graphics.drawable.Drawable deleteIcon =
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_outline);
                if (deleteIcon == null) return;
                deleteIcon.setTint(0xFFFFFFFF);

                final int iconTop    = itemView.getTop() + iconMargin;
                final int iconBottom = iconTop + dpToPx(24);

                if (dX > 0) {
                    deleteIcon.setBounds(itemView.getLeft() + iconMargin, iconTop,
                            itemView.getLeft() + iconMargin + dpToPx(24), iconBottom);
                } else if (dX < 0) {
                    deleteIcon.setBounds(itemView.getRight() - iconMargin - dpToPx(24), iconTop,
                            itemView.getRight() - iconMargin, iconBottom);
                }
                deleteIcon.draw(c);
            }

            private int dpToPx(int dp) {
                return (int) (dp * getResources().getDisplayMetrics().density);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void showClearFavoritesConfirmDialog() {
        if (getContext() == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.dialog_clear_favorites_title))
                .setMessage(getString(R.string.dialog_clear_favorites_message))
                .setPositiveButton(getString(R.string.dialog_clear_history_confirm),
                        (dialog, which) -> {
                            viewModel.clearAllFavorites();
                            FileLogger.d(TAG, "Clear all favorites confirmed.");
                        })
                .setNegativeButton(getString(R.string.dialog_cancel),
                        (dialog, which) -> dialog.dismiss())
                .show();
    }
}