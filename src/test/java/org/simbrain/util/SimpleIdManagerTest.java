package org.simbrain.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleIdManagerTest {

    @Test
    public void testIdManager() {
        SimpleIdManager manager = new SimpleIdManager(c -> 4);
        assertEquals(manager.getAndIncrementId(String.class), "String_4");
        assertEquals(manager.getAndIncrementId(String.class), "String_5");
        assertEquals(manager.getProposedId(String.class), "String_6");
    }

    @Test
    public void testIncrements() {
        SimpleIdManager.SimpleId id = new SimpleIdManager.SimpleId("Base", 1);
        id.getId();
        id.getId();
        id.getId();
        assertEquals(4, id.getCurrentIndex());
    }

    @Test
    public void testProposedId() {
        SimpleIdManager.SimpleId id = new SimpleIdManager.SimpleId("Base", 1);
        id.getId();
        id.getId();
        id.getId();
        assertEquals("Base_4", id.getProposedId());

        // The id should _not_ have been incremented by the last call
        assertEquals("Base_4", id.getId());

        // The id should have been incremented by the last call
        assertEquals("Base_5", id.getId());
    }


}