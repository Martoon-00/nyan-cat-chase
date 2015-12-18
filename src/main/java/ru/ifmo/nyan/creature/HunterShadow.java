package ru.ifmo.nyan.creature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.field.Field;

public class HunterShadow implements HunterInstance {
    protected final Field field;

    @NotNull
    protected Coord position;

    @Nullable
    protected Direction lastKnownDirection;

    public HunterShadow(Field field, @NotNull Coord position) {
        if (!field.inBounds(position))
            throw new IllegalArgumentException(String.format("Illegal initial position: %s", position));

        this.field = field;
        this.position = position;
    }

    @NotNull
    @Override
    public Coord getPosition() {
        return position;
    }

    @Nullable
    public Direction getLastDirection() {
        return lastKnownDirection;
    }

    public void setLastDirection(Direction lastKnownDirection) {
        this.lastKnownDirection = lastKnownDirection;
    }
}
