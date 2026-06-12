package com.igoy86.nexttranslate.presentation.collection;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.R;
import com.igoy86.nexttranslate.databinding.DialogCreateCollectionBinding;
import com.igoy86.nexttranslate.databinding.FragmentCollectionBinding;
import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.model.DictionaryEntry;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import android.speech.tts.TextToSpeech;
import com.igoy86.nexttranslate.domain.usecase.collection.AddWordToCollectionUseCase;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Locale;
import com.igoy86.nexttranslate.util.FileLogger;

import com.igoy86.nexttranslate.util.AppExecutors;

import java.util.List;

/**
 * Fragment for the Collections screen (entry point from the toolbar icon
 * on {@code TextFragment}).
 *
 * <p>Responsibilities:</p>
 * <ol>
 *     <li>Display the "Koleksi Saya" 2-column grid of user collections
 *         backed by Room via {@link CollectionViewModel}.</li>
 *     <li>Allow the user to create, rename, and delete collections via
 *         a dialog and popup menu.</li>
 *     <li>Provide a dictionary lookup field that queries the Free Dictionary
 *         API and displays definition results inline.</li>
 * </ol>
 */
public class CollectionFragment extends BaseFragment<FragmentCollectionBinding> {

    /** Tag used for logging events originating from this Fragment. */
    private static final String TAG = "CollectionFragment";

    /** Default accent colour used when the user has not selected one. */
    private static final String DEFAULT_COLOR = "#00897B";
	
    // -------------------------------------------------------------------------
    // ViewModel + Adapters
    // -------------------------------------------------------------------------

    /** ViewModel managing collection and dictionary state for this screen. */
    private CollectionViewModel viewModel;

    /** Adapter for the "Koleksi Saya" 2-column grid. */
    private CollectionAdapter collectionAdapter;

    /** Adapter for the definition list inside the dictionary result card. */
    private DefinitionAdapter definitionAdapter;

    /** Currently selected colour in the create/rename dialog. */
    @NonNull
    private String selectedColor = DEFAULT_COLOR;
	
	/** Android TextToSpeech engine for speaking dictionary words. */
    @Nullable
    private TextToSpeech tts;

    /** True once the TTS engine has finished initializing and is ready to use. */
    private boolean isTtsReady = false;

    /** Use case for saving a word into a user collection. */
    private AddWordToCollectionUseCase addWordToCollectionUseCase;

    // =========================================================================
    // BaseFragment contract
    // =========================================================================

    @NonNull
    @Override
    protected FragmentCollectionBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentCollectionBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initViews() {
        setupViewModel();
		
		// Back arrow
        getBinding().toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
		
        setupCollectionRecyclerView();
        setupDefinitionRecyclerView();
        initTts();
        addWordToCollectionUseCase = ((com.igoy86.nexttranslate.NextTranslateApp)
                requireActivity().getApplication()).getContainer().getAddWordToCollectionUseCase();

    }

    @Override
    protected void initObservers() {
        // --- Collections ---
        viewModel.getCollectionsLiveData().observe(getViewLifecycleOwner(), collections -> {
            if (collections == null || collections.isEmpty()) {
                getBinding().layoutEmptyCollections.setVisibility(View.VISIBLE);
                getBinding().recyclerViewCollections.setVisibility(View.GONE);
            } else {
                getBinding().layoutEmptyCollections.setVisibility(View.GONE);
                getBinding().recyclerViewCollections.setVisibility(View.VISIBLE);
                collectionAdapter.submitList(collections);
            }
        });

        // --- Dictionary loading ---
        viewModel.getDictionaryLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            getBinding().progressDictionary.setVisibility(
                    Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE
            );
            if (Boolean.TRUE.equals(isLoading)) {
                getBinding().cardDictionaryResult.setVisibility(View.GONE);
                getBinding().textViewDictionaryError.setVisibility(View.GONE);
            }
        });

        // --- Dictionary result ---
        viewModel.getDictionaryResultLiveData().observe(getViewLifecycleOwner(), entries -> {
            if (entries != null && !entries.isEmpty()) {
                bindDictionaryResult(entries);
            }
        });

        // --- Dictionary error ---
        viewModel.getDictionaryErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                getBinding().textViewDictionaryError.setText(error);
                getBinding().textViewDictionaryError.setVisibility(View.VISIBLE);
                getBinding().cardDictionaryResult.setVisibility(View.GONE);
                viewModel.clearDictionaryError();
            }
        });

        // --- Snackbar messages ---
        viewModel.getSnackbarMessageLiveData().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                showSnackbar(message);
                viewModel.clearSnackbarMessage();
            }
        });
		
		// --- Explain loading ---
        viewModel.getExplainLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            getBinding().progressExplainAi.setVisibility(
                    Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE
            );
            if (Boolean.TRUE.equals(isLoading)) {
                getBinding().cardExplainAi.setVisibility(View.GONE);
            }
        });

       // --- Explain result ---
       viewModel.getExplainResultLiveData().observe(getViewLifecycleOwner(), combined -> {
            if (combined != null) {
                final String[] parts = combined.split("\n\n---\n\n", 2);
                getBinding().textViewExplainEn.setText(parts[0]);
                getBinding().textViewExplainId.setText(parts.length > 1 ? parts[1] : "");
                getBinding().cardExplainAi.setVisibility(View.VISIBLE);
            }
        });      

        // --- Explain error ---
        viewModel.getExplainErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showSnackbar("AI Explain: " + error);
                viewModel.clearExplainError();
            }
        });
    }

    @Override
    protected void initListeners() {
		getBinding().toolbar.setNavigationOnClickListener(v ->
            requireActivity().getSupportFragmentManager().popBackStack()
        );
        // "+" button in section header
        getBinding().btnAddCollection.setOnClickListener(v -> showCreateCollectionDialog());

        // Dictionary search field — trigger lookup on keyboard "Search" action
        getBinding().etDictionarySearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performDictionarySearch();
                return true;
            }
            return false;
        });

        // Also trigger search when user taps the end icon (search icon)
        getBinding().tilDictionarySearch.setStartIconOnClickListener(v ->
            performDictionarySearch()
        );

        // End icon (X) — clear input + semua result
        getBinding().tilDictionarySearch.setEndIconOnClickListener(v -> {
            getBinding().etDictionarySearch.setText("");
            getBinding().cardDictionaryResult.setVisibility(View.GONE);
            getBinding().cardExplainAi.setVisibility(View.GONE);
            getBinding().progressExplainAi.setVisibility(View.GONE);
            getBinding().textViewDictionaryError.setVisibility(View.GONE);
            getBinding().progressDictionary.setVisibility(View.GONE);
            viewModel.clearDictionaryResult();
            viewModel.clearExplainResult();
        });
    }

    // =========================================================================
    // Setup helpers
    // =========================================================================

    /**
     * Initializes the {@link CollectionViewModel} via the factory.
     */
    private void setupViewModel() {
        final NextTranslateApp app =
                (NextTranslateApp) requireActivity().getApplication();

        final CollectionViewModelFactory factory = new CollectionViewModelFactory(
                app.getContainer().getGetCollectionsUseCase(),
                app.getContainer().getCreateCollectionUseCase(),
                app.getContainer().getDeleteCollectionUseCase(),
                app.getContainer().getRenameCollectionUseCase(),
                app.getContainer().getLookupWordUseCase(),
				app.getContainer().getExplainWordUseCase(), 
                AppExecutors.getInstance()
        );

        viewModel = new ViewModelProvider(this, factory).get(CollectionViewModel.class);
    }

    /**
     * Sets up the "Koleksi Saya" RecyclerView with a 2-column GridLayoutManager
     * and a {@link CollectionAdapter}.
     */
    private void setupCollectionRecyclerView() {
        collectionAdapter = new CollectionAdapter(new CollectionAdapter.OnCollectionClickListener() {
            @Override
            public void onCollectionClick(@NonNull CollectionItem item) {
                FileLogger.d(TAG, "Collection tapped: " + item.getName());
                if (getActivity() instanceof com.igoy86.nexttranslate.MainActivity) {
                    ((com.igoy86.nexttranslate.MainActivity) getActivity())
                        .openCollectionDetailFragment(item.getId(), item.getName(), item.getColorHex());
                }
            }

            @Override
            public boolean onCollectionLongClick(@NonNull CollectionItem item) {
                showCollectionOptionsMenu(item);
                return true;
            }
        });

        getBinding().recyclerViewCollections.setLayoutManager(
                new GridLayoutManager(requireContext(), 2)
        );
        getBinding().recyclerViewCollections.setAdapter(collectionAdapter);
    }

    /**
     * Sets up the definitions RecyclerView inside the dictionary result card.
     */
    private void setupDefinitionRecyclerView() {
        definitionAdapter = new DefinitionAdapter();
        getBinding().recyclerViewDefinitions.setAdapter(definitionAdapter);
        // LayoutManager is set inside bindDictionaryResult to avoid NPE
        // before the card becomes visible.
    }

    // =========================================================================
    // Dictionary
    // =========================================================================

    /**
     * Reads the current text in the search field and triggers a dictionary
     * lookup if the input is non-empty. Dismisses the soft keyboard.
     */
    private void performDictionarySearch() {
        final Editable text = getBinding().etDictionarySearch.getText();
        if (text == null || text.toString().trim().isEmpty()) return;

        final String word = text.toString().trim();
        hideKeyboard();
        viewModel.lookupWord(word);
    }

   /**
     * Binds dictionary lookup results to the result card.
     *
     * <p>When using {@code language=all}, the API may return multiple entries
     * from different languages. This method displays the first entry's word,
     * pronunciation, and definitions, and shows a language label indicating
     * which language the entry belongs to.</p>
     *
     * <p>If multiple entries exist across different languages, a summary line
     * is shown (e.g. "Also found in: German, Slovak").</p>
     *
     * @param entries the non-empty list of lookup results across all languages
     */
    private void bindDictionaryResult(@NonNull List<DictionaryEntry> entries) {
        // Use the first entry as the primary result
        final DictionaryEntry primary = entries.get(0);

        // Word title
        getBinding().textViewDictionaryWord.setText(primary.getWord());

        // Language label e.g. "English"
        if (!primary.getLanguageName().isEmpty()) {
            getBinding().textViewDictionaryLanguage.setText(primary.getLanguageName());
            getBinding().textViewDictionaryLanguage.setVisibility(View.VISIBLE);
        } else {
            getBinding().textViewDictionaryLanguage.setVisibility(View.GONE);
        }

        // IPA pronunciation
        if (primary.getPronunciation() != null) {
            getBinding().textViewDictionaryPronunciation.setText(primary.getPronunciation());
            getBinding().textViewDictionaryPronunciation.setVisibility(View.VISIBLE);
        } else {
            getBinding().textViewDictionaryPronunciation.setVisibility(View.GONE);
        }

        // Part of speech chip
        if (!primary.getPartOfSpeech().isEmpty()) {
            getBinding().textViewDictionaryPartOfSpeech.setText(primary.getPartOfSpeech());
            getBinding().textViewDictionaryPartOfSpeech.setVisibility(View.VISIBLE);
        } else {
            getBinding().textViewDictionaryPartOfSpeech.setVisibility(View.GONE);
        }

        // Definitions
        if (getBinding().recyclerViewDefinitions.getLayoutManager() == null) {
            getBinding().recyclerViewDefinitions.setLayoutManager(
                    new androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            );
        }
        definitionAdapter.setSenses(primary.getSenses());

        // Synonyms chips
        populateWordChips(
                primary.getSynonyms(),
                getBinding().chipGroupSynonyms,
                getBinding().labelSynonyms
        );

        // Antonyms chips
        populateWordChips(
                primary.getAntonyms(),
                getBinding().chipGroupAntonyms,
                getBinding().labelAntonyms
        );

        // "Also found in" — show other languages if more than 1 entry
        if (entries.size() > 1) {
            final StringBuilder sb = new StringBuilder("Also found in: ");
            for (int i = 1; i < entries.size(); i++) {
                final DictionaryEntry e = entries.get(i);
                if (!e.getLanguageName().isEmpty()) {
                    sb.append(e.getLanguageName());
                    if (i < entries.size() - 1) sb.append(", ");
                }
            }
            getBinding().textViewAlsoFoundIn.setText(sb.toString());
            getBinding().textViewAlsoFoundIn.setVisibility(View.VISIBLE);
        } else {
            getBinding().textViewAlsoFoundIn.setVisibility(View.GONE);
        }

        // TTS button
        getBinding().btnDictionarySpeak.setOnClickListener(v ->
                speakWord(primary.getWord())
        );
		
		// Save to collection button
        final String firstDefinition = (primary.getSenses() != null && !primary.getSenses().isEmpty())
                ? primary.getSenses().get(0).getDefinition()
                : "";
        getBinding().btnSaveToCollection.setOnClickListener(v ->
                showSaveToCollectionSheet(primary.getWord(), firstDefinition)
        );
		
		// AI Explain button
        getBinding().btnExplainAi.setOnClickListener(v ->
                viewModel.explainWord(
                        primary.getWord(),
                        primary.getLanguageName().isEmpty() ? "English" : primary.getLanguageName(),
                        firstDefinition
                )
        );
        // Reset card setiap lookup baru
        getBinding().cardExplainAi.setVisibility(View.GONE);

        // Show card, hide error
        getBinding().cardDictionaryResult.setVisibility(View.VISIBLE);
        getBinding().textViewDictionaryError.setVisibility(View.GONE);
    }

    /**
     * Populates a {@link com.google.android.material.chip.ChipGroup} with word chips.
     * Shows the group and its label if the word list is non-empty; hides both otherwise.
     *
     * @param words      the list of words to display as chips
     * @param chipGroup  the ChipGroup to populate
     * @param labelView  the section label TextView shown above the ChipGroup
     */
    private void populateWordChips(
            @NonNull List<String> words,
            @NonNull com.google.android.material.chip.ChipGroup chipGroup,
            @NonNull View labelView
    ) {
        chipGroup.removeAllViews();
        if (words.isEmpty()) {
            chipGroup.setVisibility(View.GONE);
            labelView.setVisibility(View.GONE);
            return;
        }

        // Show max 8 chips to keep the card compact
        final int max = Math.min(words.size(), 8);
        for (int i = 0; i < max; i++) {
            final Chip chip = new Chip(requireContext());
            chip.setText(words.get(i));
            chip.setClickable(false);
            chipGroup.addView(chip);
        }
        chipGroup.setVisibility(View.VISIBLE);
        labelView.setVisibility(View.VISIBLE);
    }

    /**
     * Speaks the given word using the system TTS engine via MainActivity.
     * If TTS is unavailable, silently no-ops.
     *
     * @param word the word to pronounce
     */
    private void initTts() {
        tts = new TextToSpeech(requireContext(), status -> {
            isTtsReady = (status == TextToSpeech.SUCCESS);
            FileLogger.d(TAG, "TTS init status: " + status);
        });
    }

    private void speakWord(@NonNull String word) {
        if (!isTtsReady || tts == null) {
            showSnackbar("TTS not ready.");
            return;
        }
        tts.setLanguage(Locale.ENGLISH);
        tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "dict_tts");
    }

    private void showSaveToCollectionSheet(@NonNull String word, @NonNull String definition) {
        final java.util.List<CollectionItem> collections =
                viewModel.getCollectionsLiveData().getValue();

        if (collections == null || collections.isEmpty()) {
            showSnackbar("Create a collection first.");
            return;
        }

        final BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        final android.view.View sheetView = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_save_to_collection, null);
        sheet.setContentView(sheetView);

        final androidx.recyclerview.widget.RecyclerView rv =
                sheetView.findViewById(R.id.recyclerSaveToCollection);
        final com.google.android.material.button.MaterialButton btnSave =
                sheetView.findViewById(R.id.btnConfirmSave);

        rv.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));

        final long[] selectedId = {collections.get(0).getId()};
        final int[] selectedPos = {0};

        rv.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<
                androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull android.view.ViewGroup parent, int viewType) {
                android.view.View itemView = getLayoutInflater().inflate(
                        R.layout.item_save_collection_option, parent, false);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {};
            }

            @Override
            public void onBindViewHolder(
                    @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder,
                    int position) {
                final CollectionItem item = collections.get(position);
                final android.widget.TextView tvName =
                        holder.itemView.findViewById(R.id.textCollectionName);
                final android.widget.RadioButton radio =
                        holder.itemView.findViewById(R.id.radioSelected);
                final android.view.View colorDot =
                        holder.itemView.findViewById(R.id.viewColorIndicator);

                tvName.setText(item.getName());
                radio.setChecked(position == selectedPos[0]);

                try {
                    colorDot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor(item.getColorHex())
                            )
                    );
                } catch (Exception ignored) {}

                holder.itemView.setOnClickListener(v -> {
                    final int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION || pos >= collections.size()) return;
                    selectedPos[0] = pos;
                    selectedId[0] = collections.get(selectedPos[0]).getId();
                    notifyDataSetChanged();
                });
            }

            @Override
            public int getItemCount() {
                return collections.size();
            }
        });

        btnSave.setOnClickListener(v -> {
            addWordToCollectionUseCase.execute(
                    selectedId[0], word, definition,
                    () -> {
                        showSnackbar("\"" + word + "\" saved to collection.");
                        sheet.dismiss();
                    }
            );
        });

        sheet.show();
    }

    // =========================================================================
    // Collection dialogs
    // =========================================================================

    /**
     * Shows the "Create Collection" dialog with name input + colour picker.
     */
    private void showCreateCollectionDialog() {
        selectedColor = DEFAULT_COLOR;

        final DialogCreateCollectionBinding dialogBinding =
                DialogCreateCollectionBinding.inflate(getLayoutInflater());

        // Pre-select default colour
        highlightSelectedColor(dialogBinding, selectedColor);

        // Wire colour picker circles
        setupColorPicker(dialogBinding);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_create_collection)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_create, (dialog, which) -> {
                    final Editable nameEditable = dialogBinding.etCollectionName.getText();
                    final String name = nameEditable != null
                            ? nameEditable.toString().trim()
                            : "";
                    if (!name.isEmpty()) {
                        viewModel.createCollection(name, selectedColor);
                    } else {
                        showToast(R.string.error_collection_name_empty);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Shows the "Rename Collection" dialog pre-filled with the current name.
     *
     * @param item the collection to rename
     */
    private void showRenameCollectionDialog(@NonNull CollectionItem item) {
        selectedColor = item.getColorHex();

        final DialogCreateCollectionBinding dialogBinding =
                DialogCreateCollectionBinding.inflate(getLayoutInflater());

        // Pre-fill existing name
        dialogBinding.etCollectionName.setText(item.getName());
        dialogBinding.etCollectionName.setSelection(item.getName().length());

        // Pre-select existing colour
        highlightSelectedColor(dialogBinding, selectedColor);
        setupColorPicker(dialogBinding);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_rename_collection)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    final Editable nameEditable = dialogBinding.etCollectionName.getText();
                    final String name = nameEditable != null
                            ? nameEditable.toString().trim()
                            : "";
                    if (!name.isEmpty()) {
                        viewModel.renameCollection(item.getId(), name);
                    } else {
                        showToast(R.string.error_collection_name_empty);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Shows a popup menu with "Rename" and "Delete" options for a collection.
     *
     * @param item the collection for which to show options
     */
    private void showCollectionOptionsMenu(@NonNull CollectionItem item) {
        // Find the card view in the RecyclerView to anchor the popup
        final View anchor = getBinding().recyclerViewCollections;

        final PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.action_rename);
        popup.getMenu().add(0, 2, 1, R.string.action_delete);

        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == 1) {
                showRenameCollectionDialog(item);
                return true;
            } else if (menuItem.getItemId() == 2) {
                showDeleteConfirmationDialog(item);
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Shows a confirmation dialog before deleting a collection.
     *
     * @param item the collection to delete
     */
    private void showDeleteConfirmationDialog(@NonNull CollectionItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_delete_collection)
                .setMessage(getString(R.string.dialog_msg_delete_collection, item.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.deleteCollection(item.getId())
                )
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // =========================================================================
    // Colour picker helpers
    // =========================================================================

    /**
     * Wires click listeners to all colour circle ImageViews in the dialog.
     *
     * @param dialogBinding the inflated dialog ViewBinding
     */
    private void setupColorPicker(@NonNull DialogCreateCollectionBinding dialogBinding) {
        final LinearLayout colorLayout = dialogBinding.layoutColorPicker;
        for (int i = 0; i < colorLayout.getChildCount(); i++) {
            final View child = colorLayout.getChildAt(i);
            if (child instanceof ImageView) {
                final String colorHex = (String) child.getTag();
                child.setOnClickListener(v -> {
                    selectedColor = colorHex;
                    highlightSelectedColor(dialogBinding, colorHex);
                });
            }
        }
    }

    /**
     * Visually highlights the currently selected colour circle by scaling it up
     * and resetting all others to normal size.
     *
     * @param dialogBinding the inflated dialog ViewBinding
     * @param selectedHex   the hex string of the selected colour
     */
    private void highlightSelectedColor(
            @NonNull DialogCreateCollectionBinding dialogBinding,
            @NonNull String selectedHex
    ) {
        final LinearLayout colorLayout = dialogBinding.layoutColorPicker;
        for (int i = 0; i < colorLayout.getChildCount(); i++) {
            final View child = colorLayout.getChildAt(i);
            if (child instanceof View && child.getTag() != null) {
                final String colorHex = (String) child.getTag();
                if (colorHex != null && colorHex.equals(selectedHex)) {
                    child.setScaleX(1.25f);
                    child.setScaleY(1.25f);
                } else {
                    child.setScaleX(1.0f);
                    child.setScaleY(1.0f);
                }
            }
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Hides the soft keyboard from the current focused view.
     */
    private void hideKeyboard() {
        final View view = requireActivity().getCurrentFocus();
        if (view != null) {
            final InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
	
	@Override
    public void onDestroyView() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isTtsReady = false;
        }
        super.onDestroyView();
    }
}
