package ru.ifmo.nyan.creature;

import org.jetbrains.annotations.NotNull;
import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.field.Coord;

public class NyanCat implements Creature {
    @NotNull
    private Coord position;

    public NyanCat() {
        position = new Coord(-1, -1);
    }

    @NotNull
    @Override
    public Coord getPosition() {
        return position;
    }

    public void setPosition(@NotNull Coord position) {
        this.position = position;
    }

    public void move(Direction direction) {
        position = position.add(direction.toDisplacement());
    }
}
