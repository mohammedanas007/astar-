package com.example.pathfinding;

/**
 * A rectangular grid of walkable and blocked cells.
 *
 * <p>Out-of-bounds cells are always treated as blocked by {@link #isBlocked(int, int)},
 * so callers never need a separate bounds check before asking whether a cell is usable.
 */
public final class GridMap {

    private final int width;
    private final int height;
    private final boolean[][] blocked; // indexed [y][x]

    /** Creates an all-walkable grid. */
    public GridMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Grid dimensions must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.blocked = new boolean[height][width];
    }

    /**
     * Builds a grid from text rows. '#' is a blocked cell and '.' is a walkable cell.
     * All rows must have the same length.
     */
    public static GridMap parse(String... rows) {
        if (rows == null || rows.length == 0) {
            throw new IllegalArgumentException("At least one row is required");
        }
        int w = rows[0].length();
        GridMap map = new GridMap(w, rows.length);
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != w) {
                throw new IllegalArgumentException(
                        "Row " + y + " has length " + rows[y].length() + ", expected " + w);
            }
            for (int x = 0; x < w; x++) {
                char c = rows[y].charAt(x);
                if (c == '#') {
                    map.blocked[y][x] = true;
                } else if (c != '.') {
                    throw new IllegalArgumentException(
                            "Unknown cell '" + c + "' at " + new Point(x, y));
                }
            }
        }
        return map;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean inBounds(Point p) {
        return inBounds(p.x(), p.y());
    }

    /** Returns true if the cell is blocked or lies outside the grid. */
    public boolean isBlocked(int x, int y) {
        return !inBounds(x, y) || blocked[y][x];
    }

    public boolean isBlocked(Point p) {
        return isBlocked(p.x(), p.y());
    }

    /** Marks an in-bounds cell blocked or walkable. */
    public void setBlocked(int x, int y, boolean value) {
        if (!inBounds(x, y)) {
            throw new IllegalArgumentException("Cell out of bounds: " + new Point(x, y));
        }
        blocked[y][x] = value;
    }
}
