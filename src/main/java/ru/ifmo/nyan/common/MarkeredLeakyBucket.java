package ru.ifmo.nyan.common;

import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Imitates leaky bucket with single marker.
 */
public class MarkeredLeakyBucket<T> {
    private static final Logger logger = Logger.getLogger(MarkeredLeakyBucket.class);

    private final Consumer<T> hole;
    private final Predicate<T> isBlockingItem;

    private final LinkedBlockingQueue<T> items;
    private final LinkedBlockingQueue<Boolean> markers = new LinkedBlockingQueue<>(1);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public MarkeredLeakyBucket(Consumer<T> hole) {
        this.hole = hole;
        items = new LinkedBlockingQueue<>();
        isBlockingItem = __ -> true;
    }

    public MarkeredLeakyBucket(int capacity, Consumer<T> hole) {
        this.hole = hole;
        items = new LinkedBlockingQueue<>(capacity);
        isBlockingItem = __ -> true;
    }

    public MarkeredLeakyBucket(int capacity, Consumer<T> hole, Predicate<T> isBlockingItem) {
        this.hole = hole;
        this.isBlockingItem = isBlockingItem;
        items = new LinkedBlockingQueue<>(capacity);
    }

    public void start() {
        executor.submit(new Gravity());
    }

    public void putItem(T item) {
        items.offer(item);
    }

    public void putMarker() {
        markers.offer(true);
    }

    public void putMarkerPeriodically(int initialDelay, int period) {
        executor.scheduleAtFixedRate(this::putMarker, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public void close() {
        executor.shutdownNow();
    }

    private class Gravity implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    T item = items.take();
                    boolean isBlocking = isBlockingItem.test(item);

                    try {
                        hole.accept(item);
                    } catch (Exception e) {
                        logger.error("Error on passing item to hole", e);
                    }

                    if (isBlocking) {
                        markers.take();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
