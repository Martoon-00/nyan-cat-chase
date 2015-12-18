package ru.ifmo.nyan.field;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Field {
    private final int height;

    /**
     * Columns number
     */
    private final int width;

    public Field(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Field(int size) {
        this(size, size);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Stream<Coord> allCellsCoords() {
        return Stream.iterate(0, a -> a + 1).limit(height)
                .flatMap(y -> Stream.iterate(0, a -> a + 1).limit(width)
                        .map(x -> new Coord(x, y)));
    }

    public boolean inBounds(Coord c) {
        return c.x >= 0 && c.x < width && c.y >= 0 && c.y < height ;
    }

    public void requireInBounds(Coord c) {
        if (!inBounds(c)) {
            throw new IllegalArgumentException(String.format("Coordinates %s are not within field bounds [0, %d] x [0, %d]", c, width, height));
        }
    }

    public Coord randomCell() {
        Random random = new Random();
        return new Coord(random.nextInt(width), random.nextInt(height));
    }

}
