package com.example.pathfinding;

/** An immutable (x, y) cell coordinate. x grows to the right, y grows downward. */
public record Point(int x, int y) {

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
