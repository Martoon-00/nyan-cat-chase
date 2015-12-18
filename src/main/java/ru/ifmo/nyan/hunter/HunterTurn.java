package ru.ifmo.nyan.hunter;

public interface HunterTurn {
    void move();

    default boolean endsTurn() {
        return true;
    }
}
