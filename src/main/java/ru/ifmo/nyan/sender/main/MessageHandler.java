package ru.ifmo.nyan.sender.main;

import java.net.InetSocketAddress;

public class MessageHandler<M extends Message> {
    protected final MessageSender sender;
    protected final MessageContainer<M> container;

    public MessageHandler(MessageSender sender, MessageContainer<M> container) {
        this.sender = sender;
        this.container = container;
    }

    public M getMessage() {
        return container.message;
    }

    public InetSocketAddress getSourceAddress() {
        return container.responseListenerAddress;
    }

    public void receiveAgain() {
        sender.receiveAgain(container);
    }

}
