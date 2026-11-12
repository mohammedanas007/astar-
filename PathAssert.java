package com.example.pathfinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;

/** Shared checks that a returned path is a legal route, independent of how it was found. */
final class PathAssert {

    private static final double SQRT2 = Math.sqrt(2.0);

    private PathAssert() {}

    static void assertValidPath(GridMap map, PathResult r, Point start, Point goal, boolean diagonalAllowed) {
        assertTrue("expected a path to be found", r.found());
        List<Point> p = r.path();
        assertFalse("found path must not be empty", p.isEmpty());
        assertEquals("path must begin at start", start, p.get(0));
        assertEquals("path must end at goal", goal, p.get(p.size() - 1));
        assertEquals("path must not revisit cells", p.size(), new HashSet<>(p).size());

        double sum = 0.0;
        for (int i = 0; i < p.size(); i++) {
            Point c = p.get(i);
            assertTrue("cell out of bounds: " + c, map.inBounds(c));
            assertFalse("path crosses blocked cell " + c, map.isBlocked(c));
            if (i == 0) {
                continue;
            }
            Point prev = p.get(i - 1);
            int dx = c.x() - prev.x();
            int dy = c.y() - prev.y();
            assertTrue("illegal step " + prev + "->" + c,
                    Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && (dx != 0 || dy != 0));
            if (dx != 0 && dy != 0) {
                assertTrue("diagonal step used in 4-way search: " + prev + "->" + c, diagonalAllowed);
                assertFalse("diagonal step cuts a corner: " + prev + "->" + c,
                        map.isBlocked(prev.x() + dx, prev.y()) || map.isBlocked(prev.x(), prev.y() + dy));
                sum += SQRT2;
            } else {
                sum += 1.0;
            }
        }
        assertEquals("reported cost must equal the path's own length", sum, r.cost(), 1e-9);
    }
}
