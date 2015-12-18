package ru.ifmo.nyan.judge;

import ru.ifmo.nyan.field.Coord;
import ru.ifmo.nyan.misc.Colorer;

import java.io.PrintStream;
import java.util.Optional;

public class JudgeGameVisualizer {
    private final PrintStream out;
    private final int margin;

    public JudgeGameVisualizer(PrintStream out, int margin) {
        this.out = out;
        this.margin = margin;
    }

    public void draw(JudgeGame.JudgeGameView view) {
        for (int y = 0; y < view.getField().getHeight(); y++) {
            for (int x = 0; x < view.getField().getWidth(); x++) {
                Coord curPos = new Coord(x, y);

                // try hunter
                Optional<Coord> hunterPos = view.getHuntersNearby()
                        .filter(c -> c.equals(curPos))
                        .findAny();
                if (hunterPos.isPresent()) {
                    drawCell("H", Colorer.Format.RED);
                    continue;
                }

                // try nyan
                if (view.getNyanCat().getPosition().equals(curPos)) {
                    drawCell("N", Colorer.Format.YELLOW);
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
