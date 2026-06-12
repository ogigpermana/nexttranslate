package com.igoy86.nexttranslate.presentation.collection;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.domain.usecase.collection.CreateCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.DeleteCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.GetCollectionsUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.RenameCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.dictionary.LookupWordUseCase;
import com.igoy86.nexttranslate.domain.usecase.dictionary.ExplainWordUseCase;
import com.igoy86.nexttranslate.util.AppExecutors;

/**
 * Factory for creating {@link CollectionViewModel} instances with constructor injection.
 *
 * <p>Required because {@link CollectionViewModel} has a non-default constructor.
 * Used with {@link androidx.lifecycle.ViewModelProvider} in
 * {@link CollectionFragment}.</p>
 */
public class CollectionViewModelFactory implements ViewModelProvider.Factory {

    @NonNull private final GetCollectionsUseCase getCollectionsUseCase;
    @NonNull private final CreateCollectionUseCase createCollectionUseCase;
    @NonNull private final DeleteCollectionUseCase deleteCollectionUseCase;
    @NonNull private final RenameCollectionUseCase renameCollectionUseCase;
    @NonNull private final LookupWordUseCase lookupWordUseCase;
	@NonNull private final ExplainWordUseCase explainWordUseCase;
    @NonNull private final AppExecutors appExecutors;

    /**
     * Constructs a new {@link CollectionViewModelFactory}.
     *
     * @param getCollectionsUseCase   use case for observing all collections
     * @param createCollectionUseCase use case for creating a new collection
     * @param deleteCollectionUseCase use case for deleting a collection
     * @param renameCollectionUseCase use case for renaming a collection
     * @param lookupWordUseCase       use case for dictionary word lookup
     * @param appExecutors            executor pools for threading
     */
    public CollectionViewModelFactory(
            @NonNull GetCollectionsUseCase getCollectionsUseCase,
            @NonNull CreateCollectionUseCase createCollectionUseCase,
            @NonNull DeleteCollectionUseCase deleteCollectionUseCase,
            @NonNull RenameCollectionUseCase renameCollectionUseCase,
            @NonNull LookupWordUseCase lookupWordUseCase,
			@NonNull ExplainWordUseCase explainWordUseCase,
            @NonNull AppExecutors appExecutors
    ) {
        this.getCollectionsUseCase = getCollectionsUseCase;
        this.createCollectionUseCase = createCollectionUseCase;
        this.deleteCollectionUseCase = deleteCollectionUseCase;
        this.renameCollectionUseCase = renameCollectionUseCase;
        this.lookupWordUseCase = lookupWordUseCase;
		this.explainWordUseCase = explainWordUseCase;
        this.appExecutors = appExecutors;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CollectionViewModel.class)) {
            return (T) new CollectionViewModel(
                    getCollectionsUseCase,
                    createCollectionUseCase,
                    deleteCollectionUseCase,
                    renameCollectionUseCase,
                    lookupWordUseCase,
					explainWordUseCase,  
                    appExecutors
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
