package ru.ifmo.nyan.sender.message;


import ru.ifmo.nyan.sender.util.UniqueValue;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helps to determine for specified response-message corresponding request-message.
 */
public class MessageIdentifier implements Serializable {
    private static final AtomicInteger counter = new AtomicInteger();

    public final int id;
    public final UniqueValue unique;

    public MessageIdentifier(UniqueValue unique) {
        Objects.requireNonNull(unique);

        id = counter.getAndIncrement();
        this.unique = unique;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageIdentifier that = (MessageIdentifier) o;

        return id == that.id && unique.equals(that.unique);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + unique.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("[%s #%d]", unique, id);
    }
}
