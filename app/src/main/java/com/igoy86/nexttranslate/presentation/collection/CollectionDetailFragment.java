package com.igoy86.nexttranslate.presentation.collection;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.databinding.FragmentCollectionDetailBinding;
import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * Detail screen showing all saved words inside a single collection.
 *
 * <p>Receives the target collection via arguments:</p>
 * <ul>
 *     <li>{@link #ARG_COLLECTION_ID}   — long, the Room database ID</li>
 *     <li>{@link #ARG_COLLECTION_NAME} — String, display name for the Toolbar</li>
 *     <li>{@link #ARG_COLLECTION_COLOR} — String, hex color for the accent dot</li>
 * </ul>
 *
 * <p>Navigation: opened by {@code MainActivity.openCollectionDetailFragment()},
 * pushed onto the back stack. Back press returns to {@link CollectionFragment}.</p>
 */
public class CollectionDetailFragment extends BaseFragment<FragmentCollectionDetailBinding> {

    private static final String TAG = "CollectionDetailFragment";

    // -------------------------------------------------------------------------
    // Argument keys
    // -------------------------------------------------------------------------

    public static final String ARG_COLLECTION_ID    = "arg_collection_id";
    public static final String ARG_COLLECTION_NAME  = "arg_collection_name";
    public static final String ARG_COLLECTION_COLOR = "arg_collection_color";

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private CollectionDetailViewModel viewModel;
    private CollectionWordAdapter wordAdapter;

    private long collectionId;
    private String collectionName;
    private String collectionColor;

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance with the required collection arguments.
     *
     * @param id       the collection's database ID
     * @param name     the collection's display name
     * @param colorHex the collection's accent color hex string (e.g. "#00897B")
     * @return a configured {@link CollectionDetailFragment}
     */
    @NonNull
    public static CollectionDetailFragment newInstance(
            long id,
            @NonNull String name,
            @NonNull String colorHex
    ) {
        final CollectionDetailFragment fragment = new CollectionDetailFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_COLLECTION_ID, id);
        args.putString(ARG_COLLECTION_NAME, name);
        args.putString(ARG_COLLECTION_COLOR, colorHex);
        fragment.setArguments(args);
        return fragment;
    }

    // -------------------------------------------------------------------------
    // BaseFragment contract
    // -------------------------------------------------------------------------

    @NonNull
    @Override
    protected FragmentCollectionDetailBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentCollectionDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initViews() {
        // Read arguments
        final Bundle args = getArguments();
        if (args != null) {
            collectionId    = args.getLong(ARG_COLLECTION_ID, -1L);
            collectionName  = args.getString(ARG_COLLECTION_NAME, "Collection");
            collectionColor = args.getString(ARG_COLLECTION_COLOR, "#00897B");
        }

        // Toolbar
        getBinding().toolbar.setTitle(collectionName);
        getBinding().toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Header card
        getBinding().textViewCollectionName.setText(collectionName);
        try {
            getBinding().viewCollectionColor.setBackgroundColor(
                    Color.parseColor(collectionColor)
            );
        } catch (IllegalArgumentException e) {
            getBinding().viewCollectionColor.setBackgroundColor(Color.parseColor("#00897B"));
        }

        // RecyclerView
        wordAdapter = new CollectionWordAdapter(item -> viewModel.deleteWord(item));
        getBinding().recyclerViewWords.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        getBinding().recyclerViewWords.setAdapter(wordAdapter);

        // ViewModel
        setupViewModel();
    }

    @Override
    protected void initObservers() {
        viewModel.getWordsLiveData().observe(getViewLifecycleOwner(), this::renderWords);

        viewModel.getSnackbarMessageLiveData().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                showSnackbar(message);
                viewModel.clearSnackbarMessage();
            }
        });
    }

    @Override
    protected void initListeners() {
        // No extra listeners — delete handled via adapter callback in initViews
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void setupViewModel() {
        final NextTranslateApp app =
                (NextTranslateApp) requireActivity().getApplication();

        final CollectionDetailViewModelFactory factory = new CollectionDetailViewModelFactory(
                app.getContainer().getGetWordsInCollectionUseCase(),
                app.getContainer().getCollectionWordRepository(),
                AppExecutors.getInstance()
        );

        viewModel = new ViewModelProvider(this, factory).get(CollectionDetailViewModel.class);
        viewModel.loadWords(collectionId);
    }

    /**
     * Renders the word list, toggling between empty state and the RecyclerView.
     *
     * @param words the latest list of words; may be null or empty
     */
    private void renderWords(@Nullable List<CollectionWordItem> words) {
        final boolean isEmpty = (words == null || words.isEmpty());

        // Word count label
        final int count = (words == null) ? 0 : words.size();
        getBinding().textViewWordCount.setText(count + (count == 1 ? " word" : " words"));

        // Toggle empty state vs list
        getBinding().layoutEmptyWords.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        getBinding().cardWordsList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (!isEmpty) {
            wordAdapter.submitList(words);
        }

        FileLogger.d(TAG, "renderWords: " + count + " words for collectionId=" + collectionId);
    }
}
