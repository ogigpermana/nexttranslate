package com.igoy86.nexttranslate.presentation.collection;

import androidx.recyclerview.widget.DiffUtil;

import com.igoy86.nexttranslate.domain.model.CollectionWordItem;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class CollectionWordAdapterDiffTest {

    private DiffUtil.ItemCallback<CollectionWordItem> callback;

    @Before
    public void setUp() throws Exception {
        Field field = CollectionWordAdapter.class.getDeclaredField("DIFF_CALLBACK");
        field.setAccessible(true);
        callback = (DiffUtil.ItemCallback<CollectionWordItem>) field.get(null);
    }

    @Test
    public void areItemsTheSame_sameId_returnsTrue() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 20L, "world", "farewell", 2000L);
        assertTrue(callback.areItemsTheSame(a, b));
    }

    @Test
    public void areItemsTheSame_differentId_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(2L, 10L, "hello", "greeting", 1000L);
        assertFalse(callback.areItemsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_allFieldsEqual_returnsTrue() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "greeting", 2000L);
        assertTrue(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentWord_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "world", "greeting", 1000L);
        assertFalse(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentDefinition_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "farewell", 1000L);
        assertFalse(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentAddedAt_returnsTrue() {
        // addedAt is not compared in areContentsTheSame
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "greeting", 9999L);
        assertTrue(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentCollectionId_returnsTrue() {
        // collectionId is not compared in areContentsTheSame
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 20L, "hello", "greeting", 1000L);
        assertTrue(callback.areContentsTheSame(a, b));
    }
}
