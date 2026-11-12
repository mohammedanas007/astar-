package com.example.pathfinding;

import java.util.List;

/**
 * Outcome of a path search.
 *
 * @param found         whether a path exists
 * @param path          cells from start to goal inclusive; empty when {@code found} is false
 * @param cost          total movement cost; {@link Double#POSITIVE_INFINITY} when not found
 * @param nodesExpanded number of cells taken off the open list and expanded
 */
public record PathResult(boolean found, List<Point> path, double cost, int nodesExpanded) {

    public PathResult {
        path = List.copyOf(path);
    }

    static PathResult notFound(int nodesExpanded) {
        return new PathResult(false, List.of(), Double.POSITIVE_INFINITY, nodesExpanded);
    }
}
