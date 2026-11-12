package com.example.pathfinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.Test;

/**
 * Cross-checks A* against an independently written Dijkstra search on thousands of seeded
 * random grids. Any disagreement in reachability or optimal cost is a defect in one of them.
 */
public class RandomizedOracleTest {

    private static final long SEED = 20261110L;
    private static final int GRIDS = 2000;

    @Test
    public void matchesDijkstraOracleOnRandomGrids() {
        Random rnd = new Random(SEED);
        int reachableCases = 0;
        int unreachableCases = 0;

        for (int i = 0; i < GRIDS; i++) {
            int w = 1 + rnd.nextInt(14);
            int h = 1 + rnd.nextInt(14);
            GridMap map = new GridMap(w, h);
            double density = rnd.nextDouble() * 0.45;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (rnd.nextDouble() < density) {
                        map.setBlocked(x, y, true);
                    }
                }
            }
            Point start = new Point(rnd.nextInt(w), rnd.nextInt(h));
            Point goal = new Point(rnd.nextInt(w), rnd.nextInt(h));

            for (boolean diagonal : new boolean[] {false, true}) {
                String ctx = "grid #" + i + " diagonal=" + diagonal + "\n" + dump(map, start, goal);
                PathResult r = new AStarPathfinder(map, diagonal).findPath(start, goal);
                double expected = dijkstra(map, start, goal, diagonal);

                if (Double.isInfinite(expected)) {
                    assertFalse("should be unreachable: " + ctx, r.found());
                    assertTrue(ctx, r.path().isEmpty());
                    assertTrue(ctx, Double.isInfinite(r.cost()));
                    unreachableCases++;
                } else {
                    assertTrue("should be reachable: " + ctx, r.found());
                    PathAssert.assertValidPath(map, r, start, goal, diagonal);
                    assertEquals("suboptimal or wrong cost: " + ctx, expected, r.cost(), 1e-9);
                    reachableCases++;
                }

                PathResult again = new AStarPathfinder(map, diagonal).findPath(start, goal);
                assertEquals("search must be deterministic: " + ctx, r, again);
            }
        }

        assertTrue("oracle test should exercise many reachable cases, got " + reachableCases,
                reachableCases > 1000);
        assertTrue("oracle test should exercise many blocked cases, got " + unreachableCases,
                unreachableCases > 100);
    }

    /** Plain Dijkstra with its own neighbour logic, so it shares no code with the SUT. */
    private static double dijkstra(GridMap m, Point s, Point t, boolean diagonal) {
        if (m.isBlocked(s) || m.isBlocked(t)) {
            return Double.POSITIVE_INFINITY;
        }
        int w = m.width();
        int h = m.height();
        double[] dist = new double[w * h];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[s.y() * w + s.x()] = 0.0;
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.add(new double[] {0.0, s.y() * w + s.x()});

        while (!pq.isEmpty()) {
            double[] e = pq.poll();
            int id = (int) e[1];
            if (e[0] > dist[id]) {
                continue;
            }
            int x = id % w;
            int y = id / w;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    boolean diag = dx != 0 && dy != 0;
                    if (diag && !diagonal) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (m.isBlocked(nx, ny)) {
                        continue;
                    }
                    if (diag && (m.isBlocked(x + dx, y) || m.isBlocked(x, y + dy))) {
                        continue;
                    }
                    double nd = e[0] + (diag ? Math.sqrt(2.0) : 1.0);
                    int nid = ny * w + nx;
                    if (nd < dist[nid] - 1e-12) {
                        dist[nid] = nd;
                        pq.add(new double[] {nd, nid});
                    }
                }
            }
        }
        return dist[t.y() * w + t.x()];
    }

    private static String dump(GridMap m, Point s, Point t) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < m.height(); y++) {
            for (int x = 0; x < m.width(); x++) {
                if (x == s.x() && y == s.y()) {
                    sb.append('S');
                } else if (x == t.x() && y == t.y()) {
                    sb.append('G');
                } else {
                    sb.append(m.isBlocked(x, y) ? '#' : '.');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
