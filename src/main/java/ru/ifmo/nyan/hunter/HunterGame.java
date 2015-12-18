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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class HunterGame implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(HunterGame.class);

    public static final int FIELD_SIZE = Parameters.getSilently("field.size");

    private final Field<Cell> field = new Field<>(FIELD_SIZE, FIELD_SIZE, Cell::new);
    private final CoordToIp coordToIp = new CoordToIpMapper();

    private final Hunter hunter;
    private final List<HunterShadow> shadows = new ArrayList<>();

    private final MessageSender sender;

    private final MarkeredLeakyBucket<HunterTurn> turns = new MarkeredLeakyBucket<>(Parameters.getSilently("turns.buffer"), this::actuallyMakeTurn, HunterTurn::endsTurn);

    private final List<FieldChangeListener> changeListeners = new ArrayList<>();

    /**
     * About how long hunter is staying motionlessly
     */
    int fortificationProgress = 0;

    public HunterGame(NetworkInterface networkInterface) throws IOException {
        sender = new MessageSender(networkInterface, Parameters.getProperty("port.field"));
        registerReplyProtocols();

        hunter = new Hunter(field, field.randomCoord());
        sender.joinGroup(coordToIp.toIp(hunter.getPosition()));

        sender.unfreeze();
        turns.start();
        turns.putMarkerPeriodically(0, Parameters.getProperty("delay.move.hunter"));
    }

    public void registerReplyProtocols() {
        sender.registerReplyProtocol(ReplyProtocol.react(CellMulticastMsg.class, this::onJudgeMulticast));
    }

    public void makeTurn(HunterTurn turn) {
        // we have to filter too rapid turns
        turns.putItem(turn);
        System.out.println("Turn " + turn);
    }

    private void actuallyMakeTurn(HunterTurn turn) {
        Coord pos = hunter.getPosition();
        turn.move();
        System.out.printf("Actual turn %s: %s -> %s%n", turn, pos, hunter.getPosition());
    }

    public void subscribe(FieldChangeListener onChange) {
        changeListeners.add(onChange);
    }

    private void onJudgeMulticast(RequestHandler<CellMulticastMsg, CellMulticastResponseMsg> handler) {
        CellMulticastMsg msg = handler.getMessage();
        if (msg.inRadarRange && msg.position.equals(hunter.getPosition())){
            handler.answer(new CellMulticastResponseMsg(msg.position));
        }

        System.out.println("Received");
        Stream.concat(Stream.of(hunter), shadows.stream())
                .filter(h -> h.getPosition().equals(msg.position))
                .forEach(h -> h.setLastDirection(msg.nyanCatDirection));

        HunterGameView gameView = new HunterGameView();
        fortificate();

        changeListeners.stream().forEach(listener -> {
            try {
                listener.invoke(gameView);
            } catch (Exception e) {
                logger.warn("Listener threw an exception", e);
            }
        });
    }

    private void fortificate() {
        fortificationProgress++;

        if (fortificationProgress == Parameters.getSilently("hunter.fortification.create-shadow")) {
            shadows.add(new HunterShadow(field, hunter.getPosition()));
        }
    }

    public void close() throws IOException {
        turns.close();
        sender.close();
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

        public int fortificationProgress() {
            return fortificationProgress;
        }

        public void makeTurn(HunterTurn turn) {
            HunterGame.this.makeTurn(turn);
        }

        public int fieldSize() {
            return FIELD_SIZE;
        }
    }

    public class MoveTurn implements HunterTurn {
        public final Direction direction;

        public MoveTurn(Direction direction) {
            this.direction = direction;
        }

        @Override
        public void move() {
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

                hunter.move(direction);

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
        }

        @Override
        public String toString() {
            return "Move " + direction;
        }
    }

}
