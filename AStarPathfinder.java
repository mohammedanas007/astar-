package com.example.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * A* shortest-path search on a {@link GridMap}.
 *
 * <p><b>Movement.</b> Orthogonal steps cost 1. When diagonal movement is enabled, diagonal
 * steps cost {@code sqrt(2)} and may not cut corners: a diagonal step is allowed only if both
 * orthogonally adjacent cells it passes between are walkable.
 *
 * <p><b>Heuristic.</b> Manhattan distance for 4-way movement, octile distance for 8-way
 * movement. Both are admissible and consistent, so the returned path is optimal.
 *
 * <p><b>Tie-breaking (deterministic).</b> Open nodes are ordered by lowest f = g + h, then
 * lowest h (prefers nodes closer to the goal), then insertion order (first discovered wins).
 * Neighbours are generated in the fixed order N, E, S, W, NE, SE, SW, NW, where N is y-1.
 * Equal-cost alternatives therefore always resolve the same way.
 *
 * <p><b>Special cases.</b> A start or goal outside the grid throws
 * {@link IllegalArgumentException}. A start or goal on a blocked cell returns "not found".
 * If start equals goal the result is a single-cell path with cost 0.
 */
public final class AStarPathfinder {

    static final double SQRT2 = Math.sqrt(2.0);
    private static final double EPS = 1e-9;

    // N, E, S, W, then NE, SE, SW, NW.
    private static final int[][] DIRS = {
        {0, -1}, {1, 0}, {0, 1}, {-1, 0},
        {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };

    private final GridMap map;
    private final boolean allowDiagonal;

    public AStarPathfinder(GridMap map, boolean allowDiagonal) {
        this.map = Objects.requireNonNull(map, "map");
        this.allowDiagonal = allowDiagonal;
    }

    public PathResult findPath(Point start, Point goal) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(goal, "goal");
        if (!map.inBounds(start)) {
            throw new IllegalArgumentException("Start out of bounds: " + start);
        }
        if (!map.inBounds(goal)) {
            throw new IllegalArgumentException("Goal out of bounds: " + goal);
        }
        if (map.isBlocked(start) || map.isBlocked(goal)) {
            return PathResult.notFound(0);
        }
        if (start.equals(goal)) {
            return new PathResult(true, List.of(start), 0.0, 0);
        }

        int w = map.width();
        int h = map.height();
        double[][] g = new double[h][w];
        for (double[] row : g) {
            java.util.Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        Point[][] parent = new Point[h][w];
        boolean[][] closed = new boolean[h][w];

        PriorityQueue<Node> open = new PriorityQueue<>(Node::compareTo);
        long seq = 0;
        g[start.y()][start.x()] = 0.0;
        open.add(new Node(start.x(), start.y(), 0.0, heuristic(start.x(), start.y(), goal), seq++));

        int expanded = 0;
        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed[cur.y][cur.x]) {
                continue; // stale queue entry superseded by a cheaper one
            }
            closed[cur.y][cur.x] = true;
            expanded++;

            if (cur.x == goal.x() && cur.y == goal.y()) {
                return new PathResult(true, reconstruct(parent, goal), cur.g, expanded);
            }

            int limit = allowDiagonal ? DIRS.length : 4;
            for (int i = 0; i < limit; i++) {
                int dx = DIRS[i][0];
                int dy = DIRS[i][1];
                int nx = cur.x + dx;
                int ny = cur.y + dy;
                if (map.isBlocked(nx, ny) || closed[ny][nx]) {
                    continue;
                }
                boolean diagonal = dx != 0 && dy != 0;
                if (diagonal && (map.isBlocked(cur.x + dx, cur.y) || map.isBlocked(cur.x, cur.y + dy))) {
                    continue; // no corner cutting
                }
                double tentative = cur.g + (diagonal ? SQRT2 : 1.0);
                if (tentative < g[ny][nx] - EPS) {
                    g[ny][nx] = tentative;
                    parent[ny][nx] = new Point(cur.x, cur.y);
                    open.add(new Node(nx, ny, tentative, heuristic(nx, ny, goal), seq++));
                }
            }
        }
        return PathResult.notFound(expanded);
    }

    private double heuristic(int x, int y, Point goal) {
        int dx = Math.abs(x - goal.x());
        int dy = Math.abs(y - goal.y());
        if (!allowDiagonal) {
            return dx + dy;
        }
        return Math.max(dx, dy) + (SQRT2 - 1.0) * Math.min(dx, dy);
    }

    private static List<Point> reconstruct(Point[][] parent, Point goal) {
        List<Point> path = new ArrayList<>();
        for (Point p = goal; p != null; p = parent[p.y()][p.x()]) {
            path.add(p);
        }
        Collections.reverse(path);
        return path;
    }

    /** Open-list entry. Ordered by f, then h, then discovery order. */
    private static final class Node implements Comparable<Node> {
        final int x;
        final int y;
        final double g;
        final double h;
        final long seq;

        Node(int x, int y, double g, double h, long seq) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.seq = seq;
        }

        @Override
        public int compareTo(Node o) {
            double f1 = g + h;
            double f2 = o.g + o.h;
            if (Math.abs(f1 - f2) > EPS) {
                return Double.compare(f1, f2);
            }
            if (Math.abs(h - o.h) > EPS) {
                return Double.compare(h, o.h);
            }
            return Long.compare(seq, o.seq);
        }
    }
}
