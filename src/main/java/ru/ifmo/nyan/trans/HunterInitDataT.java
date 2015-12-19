package ru.ifmo.nyan.trans;

import ru.ifmo.nyan.hunter.HunterGame;

public class HunterInitDataT {
    public final int fieldSize;

    public HunterInitDataT(HunterGame.HunterGameView view) {
        this.fieldSize = view.getField().getHeight();
    }
}
