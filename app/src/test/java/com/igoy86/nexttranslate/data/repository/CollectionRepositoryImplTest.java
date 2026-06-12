package com.igoy86.nexttranslate.data.repository;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.igoy86.nexttranslate.data.local.dao.CollectionDao;
import com.igoy86.nexttranslate.data.local.dto.CollectionWithWordCount;
import com.igoy86.nexttranslate.data.local.entity.CollectionEntity;
import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;
import com.igoy86.nexttranslate.util.AppExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
public class CollectionRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private CollectionDao collectionDao;

    @Mock
    private AppExecutors appExecutors;

    @Mock
    private Executor diskIOExecutor;

    @Mock
    private Executor mainThreadExecutor;

    private CollectionRepositoryImpl repository;

    @Before
    public void setUp() {
        lenient().when(appExecutors.diskIO()).thenReturn(diskIOExecutor);
        lenient().when(appExecutors.mainThread()).thenReturn(mainThreadExecutor);

        // Make diskIO execute immediately for testing
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(diskIOExecutor).execute(any(Runnable.class));

        repository = new CollectionRepositoryImpl(collectionDao, appExecutors);
    }

    @Test
    public void createCollection_success_callsOnInserted() {
        CollectionItem item = new CollectionItem(0L, "Travel", "#FF0000", 1000L, 0);
        when(collectionDao.insert(any(CollectionEntity.class))).thenReturn(42L);

        CollectionRepository.InsertCallback callback = mock(CollectionRepository.InsertCallback.class);
        repository.createCollection(item, callback);

        verify(callback).onInserted(42L);
        verify(callback, never()).onError(anyString());
    }

    @Test
    public void createCollection_failure_callsOnError() {
        CollectionItem item = new CollectionItem(0L, "Travel", "#FF0000", 1000L, 0);
        when(collectionDao.insert(any(CollectionEntity.class)))
                .thenThrow(new RuntimeException("DB error"));

        CollectionRepository.InsertCallback callback = mock(CollectionRepository.InsertCallback.class);
        repository.createCollection(item, callback);

        verify(callback, never()).onInserted(anyLong());
        verify(callback).onError(contains("DB error"));
    }

    @Test
    public void renameCollection_success() {
        repository.renameCollection(1L, "New Name");
        verify(collectionDao).rename(1L, "New Name");
    }

    @Test
    public void renameCollection_failure_doesNotThrow() {
        doThrow(new RuntimeException("DB error")).when(collectionDao).rename(anyLong(), anyString());
        // Should not throw
        repository.renameCollection(1L, "New Name");
    }

    @Test
    public void deleteCollection_success() {
        repository.deleteCollection(1L);
        verify(collectionDao).deleteById(1L);
    }

    @Test
    public void deleteCollection_failure_doesNotThrow() {
        doThrow(new RuntimeException("DB error")).when(collectionDao).deleteById(anyLong());
        // Should not throw
        repository.deleteCollection(1L);
    }

    @Test
    public void getAllCollections_returnsLiveData() {
        MutableLiveData<List<CollectionWithWordCount>> dbLiveData = new MutableLiveData<>();
        List<CollectionWithWordCount> dtos = new ArrayList<>();
        CollectionEntity entity = new CollectionEntity("Travel", "#FF0000", 1000L);
        entity.setId(1L);
        CollectionWithWordCount dto = new CollectionWithWordCount();
        dto.collection = entity;
        dto.wordCount = 5;
        dtos.add(dto);
        dbLiveData.setValue(dtos);

        when(collectionDao.getAllCollectionsWithWordCount()).thenReturn(dbLiveData);

        LiveData<List<CollectionItem>> result = repository.getAllCollections();
        assertNotNull(result);
    }
}
