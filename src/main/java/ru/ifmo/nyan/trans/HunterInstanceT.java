package ru.ifmo.nyan.trans;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.creature.HunterInstance;
import ru.ifmo.nyan.field.Coord;

public class HunterInstanceT {
    public final Coord position;
    public final Direction catDir;

    public HunterInstanceT(HunterInstance hunter) {
        this.position = hunter.getPosition();
        this.catDir = hunter.getLastDirection();
    }
}
