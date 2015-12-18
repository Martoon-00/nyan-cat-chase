package ru.ifmo.nyan.player;

import ru.ifmo.nyan.common.Parameters;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.judge.JudgeGame;
import ru.ifmo.nyan.judge.JudgeGameVisualizer;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JudgeSillyPlayer {
    private final JudgeGame game;

    public JudgeSillyPlayer(NetworkInterface networkInterface) throws IOException {
        this.game = new MyJudgeGame(networkInterface);


        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> new JudgeGameVisualizer(System.out, 2).draw(game.getView()),
                200, Parameters.getProperty("delay.multicast"), TimeUnit.MILLISECONDS
        );

    }

    public void play() {
        game.initiate(new Coord(7, 7));
    }

    public static void main(String[] args) throws IOException {
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        JudgeSillyPlayer player = new JudgeSillyPlayer(networkInterface);
        player.play();

    }

    private static class MyJudgeGame extends JudgeGame {
        public MyJudgeGame(NetworkInterface networkInterface) throws IOException {
            super(networkInterface);
        }
    }
}
