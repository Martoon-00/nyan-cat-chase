package ru.ifmo.nyan.player;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.common.Parameters;
import ru.ifmo.nyan.hunter.HunterGame;
import ru.ifmo.nyan.hunter.HunterGameVisualizer;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HunterManualPlayer {
    private static final HunterGameVisualizer visualizer = new HunterGameVisualizer(System.out, 2);

    private final HunterGame game;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public HunterManualPlayer(NetworkInterface networkInterface) throws IOException {
        game = new HunterGame(networkInterface);
        int moveDelay = Parameters.getProperty("delay.move.hunter");
        executor.scheduleAtFixedRate(() -> visualizer.draw(game.getView()), 100, moveDelay, TimeUnit.MILLISECONDS);

        while (true) {
            int key = System.in.read();
            Direction dir = null;

            if (key == -1)
                break;
            else if (key == 's')
                dir = Direction.DOWN;
            else if (key == 'w')
                dir = Direction.UP;
            else if (key == 'a')
                dir = Direction.LEFT;
            else if (key == 'd')
                dir = Direction.RIGHT;

            if (dir != null)
                game.makeTurn(dir);
        }
    }

    public static void main(String[] args) throws IOException {
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        HunterManualPlayer player = new HunterManualPlayer(networkInterface);

//        player.subscribe(visualizer::draw);

    }
}
