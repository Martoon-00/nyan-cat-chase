package ru.ifmo.nyan.player;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.common.Parameters;
import ru.ifmo.nyan.hunter.FieldChangeListener;
import ru.ifmo.nyan.hunter.HunterGame;
import ru.ifmo.nyan.hunter.HunterGameVisualizer;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HunterSillyPlayer {
    private static final HunterGameVisualizer visualizer = new HunterGameVisualizer(System.out, 2);

    private final HunterGame game;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public HunterSillyPlayer(NetworkInterface networkInterface) throws IOException {
        game = new HunterGame(networkInterface);
        int moveDelay = Parameters.getProperty("delay.move.hunter");
        executor.scheduleAtFixedRate(this::makeRandomTurn, moveDelay, moveDelay, TimeUnit.MILLISECONDS);
    }

    public void subscribe(FieldChangeListener listener) {
        game.subscribe(listener);
    }

    public void makeRandomTurn() {
        Direction randomDirection = Arrays.stream(Direction.values())
                .filter(direction -> direction != Direction.HERE)
                .toArray(Direction[]::new)[new Random().nextInt(4)];

        game.makeTurn(game.new MoveTurn(randomDirection));
    }

    public static void main(String[] args) throws IOException {
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        HunterSillyPlayer player = new HunterSillyPlayer(networkInterface);

        player.subscribe(visualizer::draw);

    }
}
