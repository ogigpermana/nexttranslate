package com.igoy86.nexttranslate.presentation.language;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.igoy86.nexttranslate.NextTranslateApp;
import com.igoy86.nexttranslate.databinding.FragmentLanguageBinding;
import com.igoy86.nexttranslate.di.AppContainer;
import com.igoy86.nexttranslate.domain.model.DownloadProgress;
import com.igoy86.nexttranslate.domain.model.LanguageModel;
import com.igoy86.nexttranslate.presentation.base.BaseFragment;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Fragment for the Language Manager screen.
 *
 * <p>Displays all supported ML Kit translation languages with their
 * current download status. Allows users to download new language models
 * or delete previously downloaded ones.</p>
 *
 * <p>Before starting a download, shows a dialog asking whether to use
 * Wi-Fi only or Wi-Fi + cellular — matching Google Translate's UX.</p>
 *
 * <p>Observes {@link LanguageViewModel} for the language list, download
 * results, delete results, per-item active language code, and real
 * byte-level download progress.</p>
 */
public class LanguageFragment extends BaseFragment<FragmentLanguageBinding> {

    /** Tag used for logging events originating from this Fragment. */
    private static final String TAG = "LanguageFragment";

    /** ViewModel managing the language manager UI state. */
    private LanguageViewModel viewModel;

    /** Adapter for the language list RecyclerView. */
    private LanguageAdapter languageAdapter;

    // -------------------------------------------------------------------------
    // BaseFragment implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected FragmentLanguageBinding initBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentLanguageBinding.inflate(inflater, container, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes the {@link LanguageViewModel} and sets up the
     * {@link LanguageAdapter} with the RecyclerView.</p>
     */
    @Override
    protected void initViews() {
        final AppContainer container = NextTranslateApp.getContainer();

        final LanguageViewModelFactory factory = new LanguageViewModelFactory(
                container.getGetDownloadedLanguagesUseCase(),
                container.getDownloadLanguageModelUseCase(),
                container.getDeleteLanguageModelUseCase()
        );

        viewModel = new ViewModelProvider(requireActivity(), factory).get(LanguageViewModel.class);

        // Setup RecyclerView
        languageAdapter = new LanguageAdapter(new LanguageAdapter.OnLanguageItemClickListener() {

            /**
             * Handles download button tap — shows Wi-Fi preference dialog first.
             */
            @Override
            public void onDownloadClick(@NonNull LanguageModel model) {
                showDownloadPreferenceDialog(model);
            }

            /**
             * Handles delete button tap — shows confirmation before deleting.
             */
            @Override
            public void onDeleteClick(@NonNull LanguageModel model) {
                showDeleteModelConfirmDialog(model);
            }
        });

        getBinding().recyclerViewLanguages.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        getBinding().recyclerViewLanguages.setAdapter(languageAdapter);
		
		androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
        getBinding().rootLanguage, (v, insets) -> {
            final int top = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });
		
		getBinding().toolbar.setNavigationOnClickListener(v ->
            requireActivity().onBackPressed()
        );

        FileLogger.d(TAG, "LanguageFragment views initialized.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Observes the language list, download result, delete result,
     * active language code, and download progress from {@link LanguageViewModel}.</p>
     */
    @Override
    protected void initObservers() {
        // Observe language list with download status
        viewModel.getLanguageListLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isLoading()) {
                getBinding().progressBarLanguage.setVisibility(android.view.View.VISIBLE);
            } else if (resource.isSuccess() && resource.getData() != null) {
                getBinding().progressBarLanguage.setVisibility(android.view.View.GONE);
                languageAdapter.submitList(resource.getData());
                FileLogger.d(TAG, "Language list loaded. Count: " + resource.getData().size());
            } else if (resource.isError()) {
                getBinding().progressBarLanguage.setVisibility(android.view.View.GONE);
                showSnackbar(resource.getMessage() != null
                        ? resource.getMessage()
                        : "Failed to load languages.");
                FileLogger.e(TAG, "Language list error: " + resource.getMessage());
            }
        });

        // Observe download result for Snackbar feedback
        viewModel.getDownloadResultLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess() && resource.getData() != null) {
                showSnackbar(resource.getData().toUpperCase() + " model downloaded.");
                viewModel.clearDownloadResult();
            } else if (resource.isError()) {
                showSnackbar(resource.getMessage() != null
                        ? resource.getMessage()
                        : "Download failed.");
                viewModel.clearDownloadResult();
            }
        });

        // Observe delete result for Snackbar feedback
        viewModel.getDeleteResultLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess() && resource.getData() != null) {
                showSnackbar(resource.getData().toUpperCase() + " model deleted.");
                viewModel.clearDeleteResult();
            } else if (resource.isError()) {
                showSnackbar(resource.getMessage() != null
                        ? resource.getMessage()
                        : "Delete failed.");
                viewModel.clearDeleteResult();
            }
        });

        // Observe active language code for per-item spinner/progress
        viewModel.getActiveLanguageCodeLiveData().observe(
                getViewLifecycleOwner(),
                languageAdapter::setActiveLanguageCode
        );

        // Observe real byte-level download progress → forward to adapter
        viewModel.getDownloadProgressLiveData().observe(
                getViewLifecycleOwner(),
                progress -> {
                    languageAdapter.setDownloadProgress(progress);
                    if (progress != null) {
                        FileLogger.d(TAG, "UI progress update ["
                                + progress.getLanguageCode() + "]: "
                                + progress.getPercent() + "%");
                    }
                }
        );

        // Observe error messages from BaseViewModel
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Shows a dialog asking the user whether to download using Wi-Fi only
     * or Wi-Fi + cellular, matching Google Translate's download preference UX.
     *
     * <p>The selected preference is passed directly to the ViewModel for this
     * download session. No persistent storage is used — the dialog appears
     * each time unless you add SharedPreferences later.</p>
     *
     * @param model the {@link LanguageModel} to download
     */
    private void showDownloadPreferenceDialog(@NonNull LanguageModel model) {
        if (getContext() == null) return;

        // Track the current selection: true = Wi-Fi only, false = Wi-Fi or cellular
        final boolean[] useWifiOnly = {true};

        final android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);

        final android.widget.RadioGroup radioGroup = new android.widget.RadioGroup(getContext());
        radioGroup.setOrientation(android.widget.RadioGroup.VERTICAL);

        final android.widget.RadioButton rbWifi = new android.widget.RadioButton(getContext());
        rbWifi.setText("Use Wi-Fi only");
        rbWifi.setId(android.view.View.generateViewId());
        rbWifi.setChecked(true);

        final android.widget.RadioButton rbCellular = new android.widget.RadioButton(getContext());
        rbCellular.setText("Use Wi-Fi or cellular network");
        rbCellular.setId(android.view.View.generateViewId());

        radioGroup.addView(rbWifi);
        radioGroup.addView(rbCellular);
        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
                useWifiOnly[0] = (checkedId == rbWifi.getId())
        );

        layout.addView(radioGroup);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Download " + model.getDisplayName())
                .setMessage("Translate this language even when offline by downloading "
                        + "the offline translation file.")
                .setView(layout)
                .setPositiveButton("Download", (dialog, which) -> {
                    FileLogger.d(TAG, "Download confirmed: " + model.getLanguageCode()
                            + " requireWifi=" + useWifiOnly[0]);
                    viewModel.downloadModel(model.getLanguageCode(), useWifiOnly[0]);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Shows a confirmation dialog before deleting a downloaded language model.
     *
     * <p>Only proceeds with deletion if the user explicitly confirms
     * the action.</p>
     *
     * @param model the {@link LanguageModel} whose model is to be deleted
     */
    private void showDeleteModelConfirmDialog(@NonNull LanguageModel model) {
        if (getContext() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Language Model")
                .setMessage("Delete the " + model.getDisplayName()
                        + " model? You will need to download it again to use offline.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteModel(model.getLanguageCode());
                    FileLogger.d(TAG, "Delete model confirmed: " + model.getLanguageCode());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}