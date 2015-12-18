package ru.ifmo.nyan.hunter;

import ru.ifmo.nyan.common.Direction;
import ru.ifmo.nyan.creature.HunterInstance;
import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.misc.Colorer;

import java.io.PrintStream;
import java.util.Optional;

public class HunterGameVisualizer {
    private final PrintStream out;
    private final int margin;

    public HunterGameVisualizer(PrintStream out, int margin) {
        this.out = out;
        this.margin = margin;
    }

    public void draw(HunterGame.HunterGameView view) {
        for (int y = 0; y < view.getField().getHeight(); y++) {
            for (int x = 0; x < view.getField().getWidth(); x++) {
                Coord curPos = new Coord(x, y);

                // try hunter
                if (view.hunter().getPosition().equals(curPos)) {
                    Direction direction = view.hunter().getLastDirection();
                    if (direction == null) {
                        drawCell("H", Colorer.Format.MAGENTA);
                    } else {
                        drawCell(direction.toString(), Colorer.Format.MAGENTA);
                    }
                    continue;
                }

                // try shadow
                Optional<HunterInstance> shadow = view.shadows()
                        .filter(s -> s.getPosition().equals(curPos))
                        .findAny();
                if (shadow.isPresent()) {
                    Direction direction = shadow.get().getLastDirection();
                    if (direction == null) {
                        drawCell("S", Colorer.Format.BLUE);
                    } else {
                        drawCell(direction.toString(), Colorer.Format.BLUE);
                    }
                    continue;
                }

                // nothing here
                drawCell(".", Colorer.Format.PLAIN);
            }
            out.println();
        }

        for (int i = 0; i < margin; i++) {
            out.println();
        }
    }

    private void drawCell(String string, Colorer.Format color) {
        String sym = string.substring(0, 1);
        out.print(Colorer.paint(sym, color));
    }

}
