package com.example.pathfinding;

import static com.example.pathfinding.PathAssert.assertValidPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Behavioural tests for {@link AStarPathfinder}, grouped by intent:
 * functional correctness, map boundaries, blocked/invalid input, tie-breaking and speed.
 */
public class AStarPathfinderTest {

    private static final double R2 = Math.sqrt(2.0);
    private static final double DELTA = 1e-9;

    private static Point p(int x, int y) {
        return new Point(x, y);
    }

    private static AStarPathfinder four(GridMap m) {
        return new AStarPathfinder(m, false);
    }

    private static AStarPathfinder eight(GridMap m) {
        return new AStarPathfinder(m, true);
    }

    private static void assertNoPath(PathResult r) {
        assertFalse("expected no path", r.found());
        assertTrue("no-path result must carry an empty path", r.path().isEmpty());
        assertTrue("no-path cost must be infinite", Double.isInfinite(r.cost()));
    }

    // =====================================================================
    // Functional correctness
    // =====================================================================

    @Test
    public void straightLineOnOpenGrid() {
        GridMap m = new GridMap(5, 3);
        PathResult r = four(m).findPath(p(0, 0), p(4, 0));
        assertValidPath(m, r, p(0, 0), p(4, 0), false);
        assertEquals(4.0, r.cost(), DELTA);
        assertEquals(5, r.path().size());
    }

    @Test
    public void detoursAroundAWall() {
        GridMap m = GridMap.parse(
                ".....",
                ".###.",
                ".....");
        PathResult r = four(m).findPath(p(0, 1), p(4, 1));
        assertValidPath(m, r, p(0, 1), p(4, 1), false);
        assertEquals(6.0, r.cost(), DELTA);
    }

    @Test
    public void followsAWindingCorridor() {
        GridMap m = GridMap.parse(
                ".....",
                "####.",
                ".....",
                ".####",
                ".....");
        PathResult r = four(m).findPath(p(0, 0), p(0, 4));
        assertValidPath(m, r, p(0, 0), p(0, 4), false);
        assertEquals(12.0, r.cost(), DELTA);
        assertEquals(13, r.path().size());
    }

    @Test
    public void diagonalMovementTakesTheShortcut() {
        GridMap m = new GridMap(4, 4);
        PathResult r = eight(m).findPath(p(0, 0), p(3, 3));
        assertValidPath(m, r, p(0, 0), p(3, 3), true);
        assertEquals(3 * R2, r.cost(), DELTA);
        assertEquals(4, r.path().size());
    }

    @Test
    public void fourWaySearchNeverMovesDiagonally() {
        GridMap m = new GridMap(4, 4);
        PathResult r = four(m).findPath(p(0, 0), p(3, 3));
        assertValidPath(m, r, p(0, 0), p(3, 3), false);
        assertEquals(6.0, r.cost(), DELTA);
        assertEquals(7, r.path().size());
    }

    @Test
    public void startEqualsGoalReturnsSingleCellPath() {
        GridMap m = new GridMap(3, 3);
        PathResult r = four(m).findPath(p(1, 1), p(1, 1));
        assertTrue(r.found());
        assertEquals(List.of(p(1, 1)), r.path());
        assertEquals(0.0, r.cost(), DELTA);
    }

    @Test
    public void heuristicKeepsSearchFocusedOnTheGoal() {
        // On an open row, A* with goal-biased tie-breaking only ever expands cells on the line.
        GridMap m = new GridMap(21, 21);
        PathResult r = four(m).findPath(p(0, 10), p(20, 10));
        assertValidPath(m, r, p(0, 10), p(20, 10), false);
        assertEquals(20.0, r.cost(), DELTA);
        assertEquals(21, r.nodesExpanded());
    }

    @Test
    public void mixedDiagonalAndStraightCost() {
        GridMap m = new GridMap(6, 4);
        PathResult r = eight(m).findPath(p(0, 0), p(5, 3));
        assertValidPath(m, r, p(0, 0), p(5, 3), true);
        assertEquals(2 + 3 * R2, r.cost(), DELTA);
    }

    // =====================================================================
    // Boundary: edges and corners of the map, minimal sizes
    // =====================================================================

    @Test
    public void oneByOneGrid() {
        GridMap m = new GridMap(1, 1);
        PathResult r = eight(m).findPath(p(0, 0), p(0, 0));
        assertEquals(List.of(p(0, 0)), r.path());
    }

    @Test
    public void singleColumnAndSingleRowCorridors() {
        GridMap col = new GridMap(1, 5);
        PathResult down = four(col).findPath(p(0, 0), p(0, 4));
        assertValidPath(col, down, p(0, 0), p(0, 4), false);
        assertEquals(4.0, down.cost(), DELTA);

        GridMap row = new GridMap(5, 1);
        PathResult left = eight(row).findPath(p(4, 0), p(0, 0));
        assertValidPath(row, left, p(4, 0), p(0, 0), true);
        assertEquals(4.0, left.cost(), DELTA);
    }

    @Test
    public void cornerToOppositeCorner() {
        GridMap m = new GridMap(10, 10);
        PathResult r4 = four(m).findPath(p(0, 0), p(9, 9));
        assertValidPath(m, r4, p(0, 0), p(9, 9), false);
        assertEquals(18.0, r4.cost(), DELTA);

        PathResult r8 = eight(m).findPath(p(9, 9), p(0, 0));
        assertValidPath(m, r8, p(9, 9), p(0, 0), true);
        assertEquals(9 * R2, r8.cost(), DELTA);
    }

    @Test
    public void routeMustHugTheMapEdge() {
        GridMap m = GridMap.parse(
                ".....",
                ".###.",
                ".###.",
                ".###.",
                ".....");
        PathResult r = four(m).findPath(p(0, 0), p(4, 4));
        assertValidPath(m, r, p(0, 0), p(4, 4), false);
        assertEquals(8.0, r.cost(), DELTA);
        for (Point c : r.path()) {
            assertTrue("cell " + c + " should lie on the map edge",
                    c.x() == 0 || c.x() == 4 || c.y() == 0 || c.y() == 4);
        }
    }

    @Test
    public void searchDoesNotWrapAroundMapEdges() {
        GridMap m = GridMap.parse(
                "..#..",
                "..#..");
        PathResult r = four(m).findPath(p(0, 0), p(4, 0));
        assertNoPath(r);
        assertEquals("only the 2x2 left region is reachable", 4, r.nodesExpanded());
    }

    @Test
    public void startAndGoalOnEdgesOfNarrowGap() {
        GridMap m = GridMap.parse(
                "..#..",
                ".....",
                "..#..");
        PathResult r = four(m).findPath(p(0, 0), p(4, 2));
        assertValidPath(m, r, p(0, 0), p(4, 2), false);
        assertEquals(6.0, r.cost(), DELTA);
    }

    // =====================================================================
    // Edge / error conditions: blocked paths, invalid input
    // =====================================================================

    @Test
    public void fullyWalledOffGoalHasNoPath() {
        GridMap m = GridMap.parse(
                "..#..",
                "..#..",
                "..#..");
        assertNoPath(four(m).findPath(p(0, 1), p(4, 1)));
        assertNoPath(eight(m).findPath(p(0, 1), p(4, 1)));
    }

    @Test
    public void enclosedGoalExhaustsTheReachableRegion() {
        GridMap m = GridMap.parse(
                ".....",
                ".###.",
                ".#.#.",
                ".###.",
                ".....");
        PathResult r = four(m).findPath(p(0, 0), p(2, 2));
        assertNoPath(r);
        assertEquals("all 16 border cells are reachable and expanded", 16, r.nodesExpanded());
    }

    @Test
    public void blockedStartOrGoalMeansNoPath() {
        GridMap m = GridMap.parse(
                ".#.",
                "...",
                "..#");
        PathResult blockedStart = four(m).findPath(p(1, 0), p(0, 0));
        assertNoPath(blockedStart);
        assertEquals(0, blockedStart.nodesExpanded());

        PathResult blockedGoal = four(m).findPath(p(0, 0), p(2, 2));
        assertNoPath(blockedGoal);
        assertEquals(0, blockedGoal.nodesExpanded());
    }

    @Test
    public void startAndGoalBothBlockedAndEqual() {
        GridMap m = GridMap.parse("#");
        assertNoPath(four(m).findPath(p(0, 0), p(0, 0)));
    }

    @Test
    public void outOfBoundsStartIsRejected() {
        GridMap m = new GridMap(3, 3);
        AStarPathfinder pf = four(m);
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(-1, 0), p(2, 2)));
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(0, -1), p(2, 2)));
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(3, 0), p(2, 2)));
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(0, 3), p(2, 2)));
    }

    @Test
    public void outOfBoundsGoalIsRejected() {
        GridMap m = new GridMap(3, 3);
        AStarPathfinder pf = four(m);
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(0, 0), p(-1, 0)));
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(0, 0), p(0, -1)));
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(0, 0), p(3, 0)));
        assertThrows(IllegalArgumentException.class, () -> pf.findPath(p(0, 0), p(0, 3)));
    }

    @Test
    public void nullArgumentsAreRejected() {
        GridMap m = new GridMap(2, 2);
        AStarPathfinder pf = four(m);
        assertThrows(NullPointerException.class, () -> new AStarPathfinder(null, false));
        assertThrows(NullPointerException.class, () -> pf.findPath(null, p(0, 0)));
        assertThrows(NullPointerException.class, () -> pf.findPath(p(0, 0), null));
    }

    @Test
    public void diagonalCannotSqueezeBetweenTwoDiagonalWalls() {
        GridMap m = GridMap.parse(
                ".#",
                "#.");
        assertNoPath(eight(m).findPath(p(0, 0), p(1, 1)));
    }

    @Test
    public void diagonalCannotCutACornerEvenWhenOnlyOneSideIsBlocked() {
        GridMap m = GridMap.parse(
                ".#",
                "..");
        PathResult r = eight(m).findPath(p(0, 0), p(1, 1));
        assertValidPath(m, r, p(0, 0), p(1, 1), true);
        assertEquals(2.0, r.cost(), DELTA);
        assertEquals(List.of(p(0, 0), p(0, 1), p(1, 1)), r.path());
    }

    @Test
    public void diagonalIsAllowedWhenBothFlankingCellsAreOpen() {
        // The wall at (2,0) is not beside the (0,0)->(1,1) diagonal, so the step must stay legal.
        GridMap m = GridMap.parse(
                "..#",
                "...");
        PathResult r = eight(m).findPath(p(0, 0), p(1, 1));
        assertValidPath(m, r, p(0, 0), p(1, 1), true);
        assertEquals(R2, r.cost(), DELTA);
        assertEquals(List.of(p(0, 0), p(1, 1)), r.path());
    }

    @Test
    public void diagonalDetoursAroundABlockedCellNextToTheGoal() {
        // The unobstructed octile cost would be 1 + 2*sqrt(2). The wall at (2,1) forbids the
        // final diagonals (corner cutting), so the best legal route is 3 straight + 1 diagonal.
        GridMap m = GridMap.parse(
                "....",
                "..#.",
                "....");
        PathResult r = eight(m).findPath(p(0, 2), p(3, 0));
        assertValidPath(m, r, p(0, 2), p(3, 0), true);
        assertEquals(3 + R2, r.cost(), DELTA);
    }

    // =====================================================================
    // Ties: equal-cost alternatives must resolve deterministically
    // =====================================================================

    @Test
    public void fourWayTieResolvesEastBeforeSouth() {
        // Every monotone path costs 4; the documented tie-break yields east-first then south.
        GridMap m = new GridMap(3, 3);
        PathResult r = four(m).findPath(p(0, 0), p(2, 2));
        assertValidPath(m, r, p(0, 0), p(2, 2), false);
        assertEquals(4.0, r.cost(), DELTA);
        assertEquals(List.of(p(0, 0), p(1, 0), p(2, 0), p(2, 1), p(2, 2)), r.path());
    }

    @Test
    public void eightWayTiePrefersTheDiagonalFirst() {
        // (0,0)->(1,1)->(2,1) and (0,0)->(1,0)->(2,1) both cost 1 + sqrt(2).
        GridMap m = new GridMap(3, 2);
        PathResult r = eight(m).findPath(p(0, 0), p(2, 1));
        assertValidPath(m, r, p(0, 0), p(2, 1), true);
        assertEquals(1 + R2, r.cost(), DELTA);
        assertEquals(List.of(p(0, 0), p(1, 1), p(2, 1)), r.path());
    }

    @Test
    public void equalCostRoutesAroundAnObstacleAreOptimalAndRepeatable() {
        GridMap m = GridMap.parse(
                ".....",
                "..#..",
                ".....");
        PathResult first = four(m).findPath(p(0, 1), p(4, 1));
        assertValidPath(m, first, p(0, 1), p(4, 1), false);
        assertEquals(6.0, first.cost(), DELTA);
        for (int i = 0; i < 5; i++) {
            PathResult again = four(m).findPath(p(0, 1), p(4, 1));
            assertEquals("same input must give the identical path", first, again);
        }
    }

    @Test
    public void reusingOnePathfinderGivesIdenticalResults() {
        GridMap m = GridMap.parse(
                "....",
                ".##.",
                "....");
        AStarPathfinder pf = eight(m);
        PathResult a = pf.findPath(p(0, 0), p(3, 2));
        PathResult b = pf.findPath(p(0, 0), p(3, 2));
        assertEquals(a, b);
    }

    // =====================================================================
    // Speed / scale (also guards against accidental O(n^2) behaviour)
    // =====================================================================

    @Test(timeout = 10_000)
    public void largeOpenGridFourWay() {
        GridMap m = new GridMap(500, 500);
        PathResult r = four(m).findPath(p(0, 0), p(499, 499));
        assertTrue(r.found());
        assertEquals(998.0, r.cost(), DELTA);
        assertEquals(999, r.path().size());
    }

    @Test(timeout = 10_000)
    public void largeOpenGridEightWay() {
        GridMap m = new GridMap(500, 500);
        PathResult r = eight(m).findPath(p(0, 0), p(499, 499));
        assertTrue(r.found());
        assertEquals(499 * R2, r.cost(), 1e-6);
        assertEquals(500, r.path().size());
    }

    @Test(timeout = 10_000)
    public void largeGridWithNoPathExploresTheWholeReachableRegion() {
        GridMap m = new GridMap(300, 300);
        for (int y = 0; y < 300; y++) {
            m.setBlocked(150, y, true);
        }
        PathResult r = four(m).findPath(p(0, 0), p(299, 299));
        assertNoPath(r);
        assertEquals(150 * 300, r.nodesExpanded());
    }
}
