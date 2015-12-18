package ru.ifmo.nyan.common;

import ru.ifmo.nyan.field.DeltaCoord;

import java.io.Serializable;
import java.util.Random;

public enum Direction implements Serializable {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    HERE(0, 0);

    public static final Direction[] outerDirections = new Direction[]{
            UP, LEFT, DOWN, RIGHT
    };

    private final DeltaCoord displacement;

    Direction(int deltaX, int deltaY) {
        displacement = new DeltaCoord(deltaX, deltaY);
    }

    public DeltaCoord toDisplacement() {
        return displacement;
    }

    public Direction[] outer() {
        return outerDirections;
    }
}
