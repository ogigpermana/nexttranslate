package com.igoy86.nexttranslate.presentation.collection;

import androidx.recyclerview.widget.DiffUtil;

import com.igoy86.nexttranslate.domain.model.CollectionItem;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class CollectionAdapterDiffTest {

    private DiffUtil.ItemCallback<CollectionItem> callback;

    @Before
    public void setUp() throws Exception {
        // Access the DIFF_CALLBACK from CollectionAdapter via reflection
        Field field = CollectionAdapter.class.getDeclaredField("DIFF_CALLBACK");
        field.setAccessible(true);
        callback = (DiffUtil.ItemCallback<CollectionItem>) field.get(null);
    }

    @Test
    public void areItemsTheSame_sameId_returnsTrue() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Work", "#00FF00", 2000L, 10);
        assertTrue(callback.areItemsTheSame(a, b));
    }

    @Test
    public void areItemsTheSame_differentId_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(2L, "Travel", "#FF0000", 1000L, 5);
        assertFalse(callback.areItemsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_allFieldsEqual_returnsTrue() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Travel", "#FF0000", 2000L, 5);
        assertTrue(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentName_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Work", "#FF0000", 1000L, 5);
        assertFalse(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentColor_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Travel", "#00FF00", 1000L, 5);
        assertFalse(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentWordCount_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 10);
        assertFalse(callback.areContentsTheSame(a, b));
    }

    @Test
    public void areContentsTheSame_differentCreatedAt_returnsTrue() {
        // createdAt is not compared in areContentsTheSame
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Travel", "#FF0000", 9999L, 5);
        assertTrue(callback.areContentsTheSame(a, b));
    }
}
