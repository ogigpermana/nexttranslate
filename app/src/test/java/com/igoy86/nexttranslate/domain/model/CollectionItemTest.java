package com.igoy86.nexttranslate.domain.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class CollectionItemTest {

    @Test
    public void equals_sameId_returnsTrue() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        assertEquals(a, b);
    }

    @Test
    public void equals_sameIdDifferentName_returnsTrue() {
        // equals() only compares id, so same id = equal regardless of other fields
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Work", "#FF0000", 1000L, 5);
        assertEquals(a, b);
    }

    @Test
    public void equals_differentId_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(2L, "Travel", "#FF0000", 1000L, 5);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_null_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        assertNotEquals(a, null);
    }

    @Test
    public void equals_differentType_returnsFalse() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        assertNotEquals(a, "string");
    }

    @Test
    public void equals_sameInstance_returnsTrue() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        assertEquals(a, a);
    }

    @Test
    public void hashCode_sameId_sameHashCode() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(1L, "Work", "#00FF00", 2000L, 10);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hashCode_differentId_differentHashCode() {
        CollectionItem a = new CollectionItem(1L, "Travel", "#FF0000", 1000L, 5);
        CollectionItem b = new CollectionItem(2L, "Travel", "#FF0000", 1000L, 5);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void getters_returnCorrectValues() {
        CollectionItem item = new CollectionItem(42L, "Slang", "#448AFF", 9999L, 15);
        assertEquals(42L, item.getId());
        assertEquals("Slang", item.getName());
        assertEquals("#448AFF", item.getColorHex());
        assertEquals(9999L, item.getCreatedAt());
        assertEquals(15, item.getWordCount());
    }

    @Test
    public void toString_containsAllFields() {
        CollectionItem item = new CollectionItem(1L, "Test", "#000000", 100L, 3);
        String str = item.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("name='Test'"));
        assertTrue(str.contains("colorHex='#000000'"));
        assertTrue(str.contains("wordCount=3"));
    }
}
