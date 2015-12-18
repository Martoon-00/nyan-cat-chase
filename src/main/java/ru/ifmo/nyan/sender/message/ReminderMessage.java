package ru.ifmo.nyan.sender.message;


import ru.ifmo.nyan.sender.main.RequestMessage;

import java.util.Objects;

public class ReminderMessage extends RequestMessage<VoidMessage> {
    private final ReminderIdentifier id;

    public ReminderMessage(ReminderIdentifier id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    ReminderIdentifier getId() {
        return id;
    }

    @Override
    protected boolean logOnSend() {
        return false;
    }

    @Override
    protected boolean logOnReceive(){
        return false;
    }
}
