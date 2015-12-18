package ru.ifmo.nyan.field;

import ru.ifmo.nyan.common.Direction;

import java.io.Serializable;

/**
 * Coordinates to access field elements
 */
public class Coord implements Serializable {
    /**
     * Row index
     */
    public final int x;

    /**
     * Column index
     */
    public final int y;

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Coord add(DeltaCoord delta) {
        return new Coord(x + delta.x, y + delta.y);
    }

    public DeltaCoord sub(Coord c) {
        return new DeltaCoord(x - c.x, y - c.y);
    }

    public Direction viewDirectionAt(Coord c) {
        DeltaCoord delta = c.sub(this);
        if (delta.x == 0 && delta.y == 0)
            return Direction.HERE;
        if (Math.abs(delta.x) < Math.abs(delta.y))
            return delta.y < 0 ? Direction.UP : Direction.DOWN;
        else
            return delta.x < 0 ? Direction.LEFT : Direction.RIGHT;
    }

    public int distance(Coord c) {
        return Math.abs(x - c.x) + Math.abs(y - c.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coord coord = (Coord) o;

        return x == coord.x && y == coord.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }
}
