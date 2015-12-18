package ru.ifmo.nyan.bots;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.judge.JudgeGame;
import ru.ifmo.nyan.player.NyanBot;

import java.util.Random;

public class ExampleNyanBot extends NyanBot {
    private final Random random;

    public ExampleNyanBot() {
        random = new Random();
    }

    public ExampleNyanBot(int seed) {
        random = new Random(seed);
    }

    @Override
    protected Coord getInitialCoord(JudgeGame.JudgeGameView view) {
        return view.getField().randomCell();
    }

    @Override
    protected void onTurn(JudgeGame.JudgeGameView view) {
        int randomDirIndex = random.nextInt(Direction.outerDirections.length);
        Direction dir = Direction.outerDirections[randomDirIndex];
        view.makeTurn(dir);
    }
}
