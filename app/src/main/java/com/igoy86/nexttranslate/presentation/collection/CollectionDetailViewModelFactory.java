package com.igoy86.nexttranslate.presentation.collection;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;
import com.igoy86.nexttranslate.domain.usecase.collection.GetWordsInCollectionUseCase;
import com.igoy86.nexttranslate.util.AppExecutors;

/**
 * Factory for creating {@link CollectionDetailViewModel} instances with constructor injection.
 */
public class CollectionDetailViewModelFactory implements ViewModelProvider.Factory {

    @NonNull private final GetWordsInCollectionUseCase getWordsInCollectionUseCase;
    @NonNull private final CollectionWordRepository collectionWordRepository;
    @NonNull private final AppExecutors appExecutors;

    public CollectionDetailViewModelFactory(
            @NonNull GetWordsInCollectionUseCase getWordsInCollectionUseCase,
            @NonNull CollectionWordRepository collectionWordRepository,
            @NonNull AppExecutors appExecutors
    ) {
        this.getWordsInCollectionUseCase = getWordsInCollectionUseCase;
        this.collectionWordRepository = collectionWordRepository;
        this.appExecutors = appExecutors;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CollectionDetailViewModel.class)) {
            return (T) new CollectionDetailViewModel(
                    getWordsInCollectionUseCase,
                    collectionWordRepository,
                    appExecutors
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
