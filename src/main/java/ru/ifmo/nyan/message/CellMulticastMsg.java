package ru.ifmo.nyan.message;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.sender.main.RequestMessage;

public class CellMulticastMsg extends RequestMessage<CellMulticastResponseMsg> {
    public final int multicastId;
    public final Coord position;
    public final Direction nyanCatDirection;
    public final boolean inRadarRange;

    public CellMulticastMsg(int multicastId, Coord position, Direction nyanCatDirection, boolean inRadarRange) {
        this.multicastId = multicastId;
        this.position = position;
        this.nyanCatDirection = nyanCatDirection;
        this.inRadarRange = inRadarRange;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", position, nyanCatDirection);
    }
}
