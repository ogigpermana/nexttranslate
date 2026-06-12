package com.igoy86.nexttranslate.domain.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class CollectionWordItemTest {

    @Test
    public void equals_sameFields_returnsTrue() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        assertEquals(a, b);
    }

    @Test
    public void equals_differentId_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(2L, 10L, "hello", "greeting", 1000L);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_differentWord_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "world", "greeting", 1000L);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_differentDefinition_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "farewell", 1000L);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_differentCollectionId_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 20L, "hello", "greeting", 1000L);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_differentAddedAt_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "greeting", 2000L);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_null_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        assertNotEquals(a, null);
    }

    @Test
    public void equals_differentType_returnsFalse() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        assertNotEquals(a, "string");
    }

    @Test
    public void equals_sameInstance_returnsTrue() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        assertEquals(a, a);
    }

    @Test
    public void hashCode_sameFields_sameHashCode() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hashCode_differentFields_differentHashCode() {
        CollectionWordItem a = new CollectionWordItem(1L, 10L, "hello", "greeting", 1000L);
        CollectionWordItem b = new CollectionWordItem(2L, 20L, "world", "farewell", 2000L);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void getters_returnCorrectValues() {
        CollectionWordItem item = new CollectionWordItem(5L, 42L, "serendipity", "lucky find", 9999L);
        assertEquals(5L, item.getId());
        assertEquals(42L, item.getCollectionId());
        assertEquals("serendipity", item.getWord());
        assertEquals("lucky find", item.getDefinition());
        assertEquals(9999L, item.getAddedAt());
    }

    @Test
    public void toString_containsAllFields() {
        CollectionWordItem item = new CollectionWordItem(1L, 10L, "test", "def", 100L);
        String str = item.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("collectionId=10"));
        assertTrue(str.contains("word='test'"));
        assertTrue(str.contains("definition='def'"));
    }
}
