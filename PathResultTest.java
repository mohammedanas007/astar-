package com.example.pathfinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PathResultTest {

    @Test
    public void notFoundHasEmptyPathAndInfiniteCost() {
        PathResult r = PathResult.notFound(7);
        assertFalse(r.found());
        assertTrue(r.path().isEmpty());
        assertTrue(Double.isInfinite(r.cost()));
        assertEquals(7, r.nodesExpanded());
    }

    @Test
    public void pathIsDefensivelyCopiedAndImmutable() {
        List<Point> source = new ArrayList<>(List.of(new Point(0, 0), new Point(1, 0)));
        PathResult r = new PathResult(true, source, 1.0, 2);
        source.clear();
        assertEquals(2, r.path().size());
        assertThrows(UnsupportedOperationException.class, () -> r.path().add(new Point(9, 9)));
    }

    @Test
    public void pointFormatsAsCoordinatePair() {
        assertEquals("(3,4)", new Point(3, 4).toString());
    }
}
