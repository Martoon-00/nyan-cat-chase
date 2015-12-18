package ru.ifmo.nyan.field;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Field<C extends Cell> {
    private final int height;

    /**
     * Columns number
     */
    private final int width;

    /**
     * Cells.
     * We can't create array of generic type, so list is in use
     */
    private final List<List<C>> field;

    public Field(int width, int height, Supplier<C> initialCells) {
        this.width = width;
        this.height = height;

        Supplier<List<C>> initialRows = () -> Stream.generate(initialCells).limit(width)
                .collect(Collectors.toList());
        field = Stream.generate(initialRows).limit(height)
                .collect(Collectors.toList());
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

    public C get(Coord c) {
        if (!inBounds(c))
            throw new IllegalArgumentException(String.format("Incorrect index. %s is not within [0, %d] x [0, %d]",
                    c, width - 1, height - 1));

        return field.get(c.x).get(c.y);
    }

    public synchronized void modify(Coord c, Consumer<C> modifier) {
        modifier.accept(get(c));
    }

    public Coord randomCoord() {
        Random random = new Random();
        return new Coord(random.nextInt(width), random.nextInt(height));
    }

}
