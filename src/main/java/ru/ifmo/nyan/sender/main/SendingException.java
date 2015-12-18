package ru.ifmo.nyan.sender.main;

public class SendingException extends Exception {
    private final IpAddress receiver;

    public SendingException(IpAddress receiver) {
        super(String.format("Connection to %s failed", receiver));
        this.receiver = receiver;
    }

    public IpAddress getReceiver() {
        return receiver;
    }
}
