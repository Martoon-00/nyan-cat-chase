package ru.ifmo.nyan.sender.main;

import java.io.Serializable;

public abstract class Message implements Serializable {
    protected boolean logOnSend() {
        return true;
    }

    protected boolean logOnReceive() {
        return true;
    }

    @Override
    public String toString() {
        return "Message <" + getClass().getSimpleName() + ">";
    }
}
