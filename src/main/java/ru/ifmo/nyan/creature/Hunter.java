package ru.ifmo.nyan.creature;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.field.Field;

public class Hunter extends HunterShadow {
    public Hunter(Field field, Coord initialPosition) {
        super(field, initialPosition);
    }

    public boolean move(Direction dir) {
        Coord newPosition = position.add(dir.toDisplacement());
        if (!field.inBounds(newPosition))
            return false;

        position = newPosition;
        return true;
    }
}
