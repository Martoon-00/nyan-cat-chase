package ru.ifmo.nyan.player;

import ru.ifmo.nyan.hunter.HunterGame;

import java.io.IOException;
import java.net.NetworkInterface;

public class HunterTransferPlayer {
    private final HunterGame game;

    public HunterTransferPlayer() throws IOException {
        game = new HunterGame(NetworkInterface.getByName("wlan0"));
    }


    public static void main(String[] args) throws IOException {
        new HunterTransferPlayer().game.subscribe(view -> {

        });

    }

}
