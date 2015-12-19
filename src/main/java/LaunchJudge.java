import ru.ifmo.nyan.bots.ExampleNyanBot;
import ru.ifmo.nyan.player.JudgeBotPlayer;

import java.io.IOException;
import java.net.NetworkInterface;

public class LaunchJudge {
    public static void main(String[] args) throws IOException {
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        JudgeBotPlayer player = new JudgeBotPlayer(networkInterface, new ExampleNyanBot(123));
        player.play();

    }

}
