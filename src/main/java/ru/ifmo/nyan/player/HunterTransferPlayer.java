package ru.ifmo.nyan.player;

import com.google.gson.Gson;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import ru.ifmo.nyan.hunter.HunterGame;
import ru.ifmo.nyan.trans.HunterGameViewT;
import ru.ifmo.nyan.trans.HunterInitDataT;

import java.io.*;
import java.net.NetworkInterface;

public class HunterTransferPlayer {
    private static final Logger logger = Logger.getLogger(HunterTransferPlayer.class);

    private final HunterGame game;

    private final Reader in;
    private final Writer out;
    private final Gson gson = new Gson();

    public HunterTransferPlayer(NetworkInterface networkInterface, Reader in, Writer out) throws IOException {
        game = new HunterGame(networkInterface);
        this.in = in;
        this.out = out;

        write(new HunterInitDataT(game.getView()));
        // periodically:
//        write(new HunterGameViewT(game.getView()));
    }

    public void write(Object obj) {
        try {
            out.write(gson.toJson(obj));
        } catch (IOException e) {
            logger.error("Writing to pipe failed");
        }
    }

    public static void main(String[] args) throws IOException {
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        Reader in = new InputStreamReader(System.in);
        Writer out = new OutputStreamWriter(System.out);

        HunterTransferPlayer player = new HunterTransferPlayer(networkInterface, in, out);
    }

}
