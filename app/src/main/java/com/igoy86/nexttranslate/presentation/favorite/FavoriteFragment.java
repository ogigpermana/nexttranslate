package com.igoy86.nexttranslate.presentation.favorite;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.databinding.FragmentFavoriteBinding;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Fragment for the favorite translations screen.
 *
 * <p>Displays all bookmarked translation entries in a RecyclerView ordered
 * from newest to oldest. Supports deleting individual favorite entries.</p>
 *
 * <p>Observes {@link FavoriteViewModel} for the favorites list and
 * error events.</p>
 *
 * <p>UI components handled in this Fragment:</p>
 * <ul>
 *     <li>Favorites RecyclerView</li>
 *     <li>Empty state view (shown when favorites list is empty)</li>
 * </ul>
 */
public class FavoriteFragment extends BaseFragment<FragmentFavoriteBinding> {

    /** Tag used for logging events originating from this Fragment. */
    private static final String TAG = "FavoriteFragment";

    /** ViewModel managing the favorites UI state. */
    private FavoriteViewModel viewModel;

    /** Adapter for the favorites RecyclerView. */
    private FavoriteAdapter favoriteAdapter;

    // -------------------------------------------------------------------------
    // BaseFragment implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected FragmentFavoriteBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentFavoriteBinding.inflate(inflater, container, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes the {@link FavoriteViewModel} and sets up the
     * {@link FavoriteAdapter} with the RecyclerView.</p>
     */
    @Override
    protected void initViews() {
        final AppContainer container = NextTranslateApp.getContainer();

        final FavoriteViewModelFactory factory = new FavoriteViewModelFactory(
                container.getGetAllFavoritesUseCase(),
                container.getDeleteFavoriteUseCase()
        );

        viewModel = new ViewModelProvider(this, factory).get(FavoriteViewModel.class);

        // Setup RecyclerView
        favoriteAdapter = new FavoriteAdapter(new FavoriteAdapter.OnFavoriteItemClickListener() {

            /**
             * Handles item tap — navigates back to translate screen
             * with the selected favorite item pre-filled.
             */
            @Override
            public void onItemClick(@NonNull FavoriteItem item) {
                // TODO: Navigate to TranslateFragment with pre-filled text
                FileLogger.d(TAG, "Favorite item clicked: " + item.getSourceText());
            }

            /**
             * Handles delete tap — removes the selected favorite entry.
             */
            @Override
            public void onDeleteClick(@NonNull FavoriteItem item) {
                viewModel.deleteFavorite(item.getId());
                FileLogger.d(TAG, "Delete favorite requested: id=" + item.getId());
            }
        });

        getBinding().recyclerViewFavorites.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        getBinding().recyclerViewFavorites.setAdapter(favoriteAdapter);
		        getBinding().toolbar.setNavigationOnClickListener(v ->
                requireActivity().onBackPressed()
        );

        FileLogger.d(TAG, "FavoriteFragment views initialized.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Observes the favorites list from {@link FavoriteViewModel} and
     * updates the RecyclerView adapter and empty state visibility.</p>
     */
    @Override
    protected void initObservers() {
        // Observe favorites list
        viewModel.getFavoritesListLiveData().observe(getViewLifecycleOwner(), favoriteItems -> {
            favoriteAdapter.submitList(favoriteItems);

            // Toggle empty state visibility
            if (favoriteItems == null || favoriteItems.isEmpty()) {
                getBinding().layoutEmptyState.setVisibility(android.view.View.VISIBLE);
                getBinding().recyclerViewFavorites.setVisibility(android.view.View.GONE);
                FileLogger.d(TAG, "Favorites list is empty.");
            } else {
                getBinding().layoutEmptyState.setVisibility(android.view.View.GONE);
                getBinding().recyclerViewFavorites.setVisibility(android.view.View.VISIBLE);
                FileLogger.d(TAG, "Favorites list updated. Count: " + favoriteItems.size());
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
     * <p>No additional listeners required for this Fragment beyond
     * those set up in the adapter callbacks.</p>
     */
    @Override
    protected void initListeners() {
        // No additional listeners required
    }
}