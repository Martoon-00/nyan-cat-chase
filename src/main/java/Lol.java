import java.io.*;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Created by martoon on 19.12.15.
 */
public class Lol {
    public static void main(String[] args) throws InterruptedException, IOException {
        Iterator<Integer> iterator = Stream.iterate(0, a -> a + 1)
                .iterator();

        Iterable<Integer> it = () -> iterator;
        try (PrintStream out = new PrintStream("lol.txt")) {
            for (Integer k : it) {
                out.print(k);
            }
        }

    }
}
