package ru.ifmo.nyan.common;

import ru.ifmo.nyan.field.DeltaCoord;

import java.io.Serializable;

public enum Direction implements Serializable {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    HERE(0, 0);

    private final DeltaCoord displacement;

    Direction(int deltaX, int deltaY) {
        displacement = new DeltaCoord(deltaX, deltaY);
    }

    public DeltaCoord toDisplacement() {
        return displacement;
    }
}
