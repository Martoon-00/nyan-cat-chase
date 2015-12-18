package ru.ifmo.nyan.judge;

import org.apache.log4j.Logger;
import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.common.MarkeredLeakyBucket;
import ru.ifmo.nyan.common.Parameters;
import ru.ifmo.nyan.creature.Creature;
import ru.ifmo.nyan.creature.NyanCat;
import ru.ifmo.nyan.field.*;
import ru.ifmo.nyan.message.CellMulticastMsg;
import ru.ifmo.nyan.sender.main.DispatchType;
import ru.ifmo.nyan.sender.main.IpAddress;
import ru.ifmo.nyan.sender.main.MessageSender;
import ru.ifmo.nyan.sender.util.UniqueValue;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class JudgeGame implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(JudgeGame.class);

    private final Field field = new Field(Parameters.getSilently("field.size"));
    private final CoordToIp coordToIp = new CoordToIpMapper();

    private final MessageSender sender;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final MarkeredLeakyBucket<Direction> nyanCatMoves = new MarkeredLeakyBucket<>(Parameters.getSilently("turns.buffer"), this::actuallyMoveNyanCat);

    private final NyanCat nyanCat = new NyanCat();
    private List<Coord> huntersNearby = new ArrayList<>();
    private List<Coord> collectingHuntersNearby = new ArrayList<>();
    private int multicastId = 0;

    private final int fieldPort;

    private boolean initiated;

    public JudgeGame(NetworkInterface networkInterface) throws IOException {
        this.fieldPort = Parameters.getProperty("port.field");
        int judgePort = Parameters.getProperty("port.judge");
        sender = new MessageSender(networkInterface, judgePort, new UniqueValue(new byte[6], "Judge"));
        sender.unfreeze();
    }

    public void initiate(Coord nyanCatInitialPosition) {
        if (initiated)
            throw new IllegalStateException("Already initiated");
        initiated = true;

        nyanCat.setPosition(nyanCatInitialPosition);
        executor.scheduleAtFixedRate(this::multicast, 0, Parameters.getSilently("delay.multicast"), TimeUnit.MILLISECONDS);
        nyanCatMoves.start();
        nyanCatMoves.putMarkerPeriodically(0, Parameters.getSilently("delay.move.cat"));
    }

    public void multicast() {
        huntersNearby = collectingHuntersNearby;
        collectingHuntersNearby = new ArrayList<>();
        multicastId++;
        field.allCellsCoords().forEach(JudgeGame.this::multicastAtCoord);
    }

    private void multicastAtCoord(Coord coord) {
        IpAddress address = new IpAddress(coordToIp.toIp(coord), fieldPort);
        Coord nyanCatPos = nyanCat.getPosition();
        CellMulticastMsg message = new CellMulticastMsg(multicastId, coord, coord.viewDirectionAt(nyanCatPos),
                coord.distance(nyanCatPos) <= Parameters.getSilently("nyan.radar.range"));

        sender.send(address, message, DispatchType.UDP, ((int) (Parameters.getSilently("delay.multicast") * 0.9)), handler ->
                collectingHuntersNearby.add(handler.getMessage().hunterPosition));
    }

    public void moveNyanCat(Direction direction) {
        nyanCatMoves.putItem(direction);
    }

    private void actuallyMoveNyanCat(Direction direction) {
        nyanCat.move(direction);
    }

    @Override
    public void close() throws Exception {
        sender.close();
        executor.shutdownNow();
    }

    public JudgeGameView getView() {
        return new JudgeGameView();
    }

    public class JudgeGameView {
        public Creature getNyanCat() {
            return nyanCat;
        }

        public void makeTurn(Direction direction) {
            JudgeGame.this.moveNyanCat(direction);
        }

        public Stream<Coord> getHuntersNearby() {
            return huntersNearby.stream();
        }

        public Field getField() {
            return field;
        }
    }

}
