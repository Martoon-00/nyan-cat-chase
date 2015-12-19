package ru.ifmo.nyan.hunter;

import org.apache.log4j.Logger;
import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.common.MarkeredLeakyBucket;
import ru.ifmo.nyan.common.Parameters;
import ru.ifmo.nyan.creature.Hunter;
import ru.ifmo.nyan.creature.HunterInstance;
import ru.ifmo.nyan.creature.HunterShadow;
import ru.ifmo.nyan.field.*;
import ru.ifmo.nyan.message.CellMulticastMsg;
import ru.ifmo.nyan.message.CellMulticastResponseMsg;
import ru.ifmo.nyan.sender.listeners.ReplyProtocol;
import ru.ifmo.nyan.sender.main.MessageSender;
import ru.ifmo.nyan.sender.main.RequestHandler;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class HunterGame implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(HunterGame.class);

    private final Field field = new Field(Parameters.getSilently("field.size"));
    private final CoordToIp coordToIp = new CoordToIpMapper();

    private final Hunter hunter;
    private final List<HunterShadow> shadows = new ArrayList<>();

    private final MessageSender sender;

    private final MarkeredLeakyBucket<Direction> turns = new MarkeredLeakyBucket<>(Parameters.getSilently("turns.buffer"), this::actuallyMakeTurn);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * About how long hunter is staying motionlessly
     */
    int fortificationProgress = 0;

    public HunterGame(NetworkInterface networkInterface) throws IOException {
        sender = new MessageSender(networkInterface, Parameters.getProperty("port.field"));
        registerReplyProtocols();

        hunter = new Hunter(field, field.randomCell());
        sender.joinGroup(coordToIp.toIp(hunter.getPosition()));

        sender.unfreeze();
        turns.start();

        int hunterTurnDelay = Parameters.getProperty("delay.move.hunter");
        turns.putMarkerPeriodically(0, hunterTurnDelay);
        executor.scheduleAtFixedRate(this::fortificate, 0, hunterTurnDelay, TimeUnit.MILLISECONDS);
    }

    public void registerReplyProtocols() {
        sender.registerReplyProtocol(ReplyProtocol.react(CellMulticastMsg.class, this::onJudgeMulticast));
    }

    public void makeTurn(Direction turn) {
        // we have to filter too rapid turns
        turns.putItem(turn);
    }

    private void actuallyMakeTurn(Direction turn) {
        Coord pos = hunter.getPosition();
        Predicate<Coord> hasShadowOn = coord -> shadows.stream()
                .filter(shadow -> shadow.getPosition().equals(coord))
                .map(shadow -> true)
                .findAny().orElse(false);

        sender.freeze();
        try {
            // change position & subscribe/unsubscribe
            Coord wasPosition = hunter.getPosition();
            if (!hasShadowOn.test(wasPosition)) {
                sender.leaveGroup(coordToIp.toIp(wasPosition));
            }

            hunter.move(turn);

            Coord newPosition = hunter.getPosition();
            if (!hasShadowOn.test(newPosition)) {
                sender.joinGroup(coordToIp.toIp(newPosition));
            }

            if (!newPosition.equals(wasPosition)) {
                fortificationProgress = 0;
            }

            registerReplyProtocols();
        } catch (IOException e) {
            logger.warn("Exception while joining/leaving group", e);
        } catch (Throwable e) {
            logger.error("Error while moving hunter");
        } finally {
            sender.unfreeze();
        }

        System.out.printf("Actual turn %s: %s -> %s%n", turn, pos, hunter.getPosition());
    }

    private void onJudgeMulticast(RequestHandler<CellMulticastMsg, CellMulticastResponseMsg> handler) {
        CellMulticastMsg msg = handler.getMessage();
        if (msg.inRadarRange && msg.position.equals(hunter.getPosition())){
            handler.answer(new CellMulticastResponseMsg(msg.position));
        }

        Stream.concat(Stream.of(hunter), shadows.stream())
                .filter(h -> h.getPosition().equals(msg.position))
                .forEach(h -> h.setLastDirection(msg.multicastId, msg.nyanCatDirection));
    }

    private void fortificate() {
        fortificationProgress++;

        if (fortificationProgress == Parameters.getSilently("hunter.fortification.create-shadow")
                && shadows.size() < Parameters.getSilently("hunter.max-shadows")) {
            // raises exception when join to many groups
            shadows.add(new HunterShadow(field, hunter.getPosition()));
        }
    }

    public void close() throws IOException {
        turns.close();
        sender.close();
    }

    public HunterGameView getView() {
        return new HunterGameView();
    }

    public class HunterGameView {
        public HunterInstance hunter() {
            return hunter;
        }

        public Stream<HunterInstance> shadows() {
            // What? Are you kidding? Why do I need to use such mapping
            return shadows.stream().map(Function.identity());
        }

        /**
         * @return both hunter & shadows
         */
        public Stream<HunterInstance> hunterInstances() {
            return Stream.concat(Stream.of(hunter), shadows.stream());
        }

        public int getFortificationProgress() {
            return fortificationProgress;
        }

        public void makeTurn(Direction turn) {
            HunterGame.this.makeTurn(turn);
        }

        public Field getField() {
            return field;
        }
    }

}
