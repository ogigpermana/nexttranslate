package com.igoy86.nexttranslate.presentation.collection;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;
import com.igoy86.nexttranslate.domain.usecase.collection.CreateCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.DeleteCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.GetCollectionsUseCase;
import com.igoy86.nexttranslate.domain.usecase.collection.RenameCollectionUseCase;
import com.igoy86.nexttranslate.domain.usecase.dictionary.ExplainWordUseCase;
import com.igoy86.nexttranslate.domain.usecase.dictionary.LookupWordUseCase;
import com.igoy86.nexttranslate.util.AppExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CollectionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private GetCollectionsUseCase getCollectionsUseCase;

    @Mock
    private CreateCollectionUseCase createCollectionUseCase;

    @Mock
    private DeleteCollectionUseCase deleteCollectionUseCase;

    @Mock
    private RenameCollectionUseCase renameCollectionUseCase;

    @Mock
    private LookupWordUseCase lookupWordUseCase;

    @Mock
    private ExplainWordUseCase explainWordUseCase;

    @Mock
    private AppExecutors appExecutors;

    @Mock
    private Executor mainThreadExecutor;

    private MutableLiveData<List<CollectionItem>> collectionsLiveData;

    private CollectionViewModel viewModel;

    @Before
    public void setUp() {
        collectionsLiveData = new MutableLiveData<>();
        when(getCollectionsUseCase.execute()).thenReturn(collectionsLiveData);

        viewModel = new CollectionViewModel(
                getCollectionsUseCase,
                createCollectionUseCase,
                deleteCollectionUseCase,
                renameCollectionUseCase,
                lookupWordUseCase,
                explainWordUseCase,
                appExecutors
        );
    }

    @Test
    public void getCollectionsLiveData_returnsLiveData() {
        assertNotNull(viewModel.getCollectionsLiveData());
    }

    @Test
    public void getCollectionsLiveData_emitsCollections() {
        List<CollectionItem> items = new ArrayList<>();
        items.add(new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5));
        items.add(new CollectionItem(2L, "Work", "#00FF00", 2000L, 10));

        collectionsLiveData.setValue(items);

        Observer<List<CollectionItem>> observer = mock(Observer.class);
        viewModel.getCollectionsLiveData().observeForever(observer);

        ArgumentCaptor<List<CollectionItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(observer).onChanged(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    public void createCollection_callsUseCase() {
        viewModel.createCollection("Travel", "#FF0000");
        verify(createCollectionUseCase).execute(eq("Travel"), eq("#FF0000"), any());
    }

    @Test
    public void renameCollection_callsUseCase() {
        viewModel.renameCollection(1L, "New Name");
        verify(renameCollectionUseCase).execute(1L, "New Name");
    }

    @Test
    public void deleteCollection_callsUseCase() {
        viewModel.deleteCollection(1L);
        verify(deleteCollectionUseCase).execute(1L);
    }

    @Test
    public void clearSnackbarMessage_setsNull() {
        viewModel.clearSnackbarMessage();
        assertNull(viewModel.getSnackbarMessageLiveData().getValue());
    }

    @Test
    public void clearDictionaryError_setsNull() {
        viewModel.clearDictionaryError();
        assertNull(viewModel.getDictionaryErrorLiveData().getValue());
    }

    @Test
    public void clearDictionaryResult_setsNull() {
        viewModel.clearDictionaryResult();
        assertNull(viewModel.getDictionaryResultLiveData().getValue());
    }

    @Test
    public void clearExplainError_setsNull() {
        viewModel.clearExplainError();
        assertNull(viewModel.getExplainErrorLiveData().getValue());
    }

    @Test
    public void clearExplainResult_setsNull() {
        viewModel.clearExplainResult();
        assertNull(viewModel.getExplainResultLiveData().getValue());
    }

    @Test
    public void lookupWord_emptyString_doesNotCallUseCase() {
        viewModel.lookupWord("   ");
        verifyNoInteractions(lookupWordUseCase);
    }

    @Test
    public void explainWord_emptyString_doesNotCallUseCase() {
        viewModel.explainWord("   ", "English", null);
        verifyNoInteractions(explainWordUseCase);
    }
}
