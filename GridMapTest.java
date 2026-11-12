package com.example.pathfinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GridMapTest {

    // ---- functional ----

    @Test
    public void parseReadsWallsAndOpenCells() {
        GridMap m = GridMap.parse(
                ".#.",
                "..#");
        assertEquals(3, m.width());
        assertEquals(2, m.height());
        assertFalse(m.isBlocked(0, 0));
        assertTrue(m.isBlocked(1, 0));
        assertFalse(m.isBlocked(2, 0));
        assertFalse(m.isBlocked(0, 1));
        assertFalse(m.isBlocked(1, 1));
        assertTrue(m.isBlocked(2, 1));
    }

    @Test
    public void newGridIsFullyWalkable() {
        GridMap m = new GridMap(4, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                assertFalse(m.isBlocked(x, y));
            }
        }
    }

    @Test
    public void setBlockedTogglesACell() {
        GridMap m = new GridMap(3, 3);
        m.setBlocked(1, 1, true);
        assertTrue(m.isBlocked(1, 1));
        assertTrue(m.isBlocked(new Point(1, 1)));
        m.setBlocked(1, 1, false);
        assertFalse(m.isBlocked(1, 1));
    }

    // ---- boundary ----

    @Test
    public void inBoundsAcceptsEveryEdgeAndRejectsOneBeyond() {
        GridMap m = new GridMap(4, 3);
        assertTrue(m.inBounds(0, 0));
        assertTrue(m.inBounds(3, 2));
        assertTrue(m.inBounds(new Point(3, 0)));
        assertTrue(m.inBounds(0, 2));
        assertFalse(m.inBounds(-1, 0));
        assertFalse(m.inBounds(0, -1));
        assertFalse(m.inBounds(4, 0));
        assertFalse(m.inBounds(0, 3));
        assertFalse(m.inBounds(new Point(4, 3)));
    }

    @Test
    public void outOfBoundsCellsCountAsBlocked() {
        GridMap m = new GridMap(2, 2);
        assertTrue(m.isBlocked(-1, 0));
        assertTrue(m.isBlocked(0, -1));
        assertTrue(m.isBlocked(2, 0));
        assertTrue(m.isBlocked(0, 2));
        assertTrue(m.isBlocked(new Point(5, 5)));
    }

    @Test
    public void minimalOneByOneGridIsAllowed() {
        GridMap m = new GridMap(1, 1);
        assertTrue(m.inBounds(0, 0));
        assertFalse(m.isBlocked(0, 0));
    }

    // ---- error / invalid input ----

    @Test
    public void nonPositiveDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GridMap(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new GridMap(5, 0));
        assertThrows(IllegalArgumentException.class, () -> new GridMap(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new GridMap(5, -1));
    }

    @Test
    public void setBlockedOutsideGridIsRejected() {
        GridMap m = new GridMap(2, 2);
        assertThrows(IllegalArgumentException.class, () -> m.setBlocked(2, 0, true));
        assertThrows(IllegalArgumentException.class, () -> m.setBlocked(0, -1, true));
    }

    @Test
    public void parseRejectsRaggedRows() {
        assertThrows(IllegalArgumentException.class, () -> GridMap.parse("...", ".."));
    }

    @Test
    public void parseRejectsUnknownCharacters() {
        assertThrows(IllegalArgumentException.class, () -> GridMap.parse(".x."));
    }

    @Test
    public void parseRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> GridMap.parse());
        assertThrows(IllegalArgumentException.class, () -> GridMap.parse((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> GridMap.parse(""));
    }
}
