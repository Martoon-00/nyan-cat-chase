package ru.ifmo.nyan.player;

import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.judge.JudgeGame;

public abstract class NyanBot {
    /**
     * Coordinate at which initially place nyan-cat
     */
    protected abstract Coord getInitialCoord(JudgeGame.JudgeGameView view);

    /**
     * When ready do make turn, this method is invoked.
     * No sense in making several turns inside this method, only first is applied
     */
    protected abstract void onTurn(JudgeGame.JudgeGameView view);

}
