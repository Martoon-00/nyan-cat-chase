package ru.ifmo.nyan.player;

import org.apache.log4j.Logger;
import ru.ifmo.nyan.common.Parameters;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.field.Field;
import ru.ifmo.nyan.judge.JudgeGame;
import ru.ifmo.nyan.judge.JudgeGameVisualizer;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JudgeBotPlayer {
    private static final Logger logger = Logger.getLogger(JudgeBotPlayer.class);

    private final JudgeGame game;
    private final NyanBot bot;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final JudgeGameVisualizer visualizer = new JudgeGameVisualizer(System.out, 2);

    public JudgeBotPlayer(NetworkInterface networkInterface, NyanBot bot) throws IOException {
        this.bot = bot;
        this.game = new JudgeGame(networkInterface);
    }

    public void play() {
        // initiate, set initial nyan-cat position
        JudgeGame.JudgeGameView gameView = game.getView();
        Coord initialCoord = bot.getInitialCoord(gameView);
        gameView.getField().requireInBounds(initialCoord);
        game.initiate(bot.getInitialCoord(game.getView()));

        int turnDelay = Parameters.getSilently("delay.move.cat");
        // draw visualization periodically
        executor.scheduleAtFixedRate(() -> visualizer.draw(game.getView()),
                0, turnDelay, TimeUnit.MILLISECONDS);

        // move periodically
        executor.scheduleAtFixedRate(this::makeTurn, turnDelay, turnDelay, TimeUnit.MILLISECONDS);

    }

    public void makeTurn() {
        try {
            bot.onTurn(game.getView());
        } catch (Exception e) {
            logger.error("Error while making turn", e);
        }
    }

}
