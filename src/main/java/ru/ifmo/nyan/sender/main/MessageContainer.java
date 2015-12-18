package ru.ifmo.nyan.sender.main;


import ru.ifmo.nyan.sender.message.MessageIdentifier;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class MessageContainer<M extends Message> implements Serializable {
    // Containers with response messages copy identifier from corresponding request
    public final MessageIdentifier identifier;
    public final InetSocketAddress responseListenerAddress;
    public final M message;

    public MessageContainer(MessageIdentifier identifier, InetSocketAddress responseListenerAddress, M message) {
        this.identifier = identifier;
        this.responseListenerAddress = responseListenerAddress;
        this.message = message;
    }
}
