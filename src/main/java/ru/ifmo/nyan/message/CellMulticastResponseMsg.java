package ru.ifmo.nyan.message;

import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.sender.main.ResponseMessage;

public class CellMulticastResponseMsg extends ResponseMessage {
    public final Coord hunterPosition;

    public CellMulticastResponseMsg(Coord hunterPosition) {
        this.hunterPosition = hunterPosition;
    }
}
