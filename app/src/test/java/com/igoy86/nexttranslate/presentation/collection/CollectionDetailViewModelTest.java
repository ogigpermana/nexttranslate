package com.igoy86.nexttranslate.presentation.collection;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;
import com.igoy86.nexttranslate.domain.usecase.collection.GetWordsInCollectionUseCase;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CollectionDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private GetWordsInCollectionUseCase getWordsInCollectionUseCase;

    @Mock
    private CollectionWordRepository collectionWordRepository;

    @Mock
    private AppExecutors appExecutors;

    @Mock
    private Executor diskIOExecutor;

    @Mock
    private Executor mainThreadExecutor;

    private CollectionDetailViewModel viewModel;

    @Before
    public void setUp() {
        // Make executors execute immediately
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(diskIOExecutor).execute(any(Runnable.class));

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mainThreadExecutor).execute(any(Runnable.class));

        when(appExecutors.diskIO()).thenReturn(diskIOExecutor);
        when(appExecutors.mainThread()).thenReturn(mainThreadExecutor);

        viewModel = new CollectionDetailViewModel(
                getWordsInCollectionUseCase,
                collectionWordRepository,
                appExecutors
        );
    }

    @Test
    public void getWordsLiveData_returnsLiveData() {
        assertNotNull(viewModel.getWordsLiveData());
    }

    @Test
    public void loadWords_callsUseCase() {
        MutableLiveData<List<CollectionWordItem>> liveData = new MutableLiveData<>();
        when(getWordsInCollectionUseCase.execute(1L)).thenReturn(liveData);

        viewModel.loadWords(1L);

        verify(getWordsInCollectionUseCase).execute(1L);
    }

    @Test
    public void loadWords_emitsWords() {
        MutableLiveData<List<CollectionWordItem>> liveData = new MutableLiveData<>();
        List<CollectionWordItem> words = new ArrayList<>();
        words.add(new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L));
        words.add(new CollectionWordItem(2L, 10L, "world", "earth", 2000L));
        liveData.setValue(words);

        when(getWordsInCollectionUseCase.execute(1L)).thenReturn(liveData);

        viewModel.loadWords(1L);

        Observer<List<CollectionWordItem>> observer = mock(Observer.class);
        viewModel.getWordsLiveData().observeForever(observer);

        ArgumentCaptor<List<CollectionWordItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(observer).onChanged(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    public void deleteWord_callsRepository() {
        CollectionWordItem item = new CollectionWordItem(5L, 10L, "hello", "greeting", 1000L);
        viewModel.deleteWord(item);
        verify(collectionWordRepository).deleteWord(5L);
    }

    @Test
    public void deleteWord_showsSnackbar() {
        CollectionWordItem item = new CollectionWordItem(5L, 10L, "hello", "greeting", 1000L);

        Observer<String> observer = mock(Observer.class);
        viewModel.getSnackbarMessageLiveData().observeForever(observer);

        viewModel.deleteWord(item);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(observer).onChanged(captor.capture());
        assertTrue(captor.getValue().contains("hello"));
        assertTrue(captor.getValue().contains("removed"));
    }

    @Test
    public void clearSnackbarMessage_setsNull() {
        viewModel.clearSnackbarMessage();
        assertNull(viewModel.getSnackbarMessageLiveData().getValue());
    }
}
