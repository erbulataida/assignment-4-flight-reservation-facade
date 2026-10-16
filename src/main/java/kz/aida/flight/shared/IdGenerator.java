package kz.aida.flight.shared;
import java.nio.file.Path;
import java.util.*;
public class IdGenerator {
    private final CsvStore store;
    public IdGenerator(Path directory) { store = new CsvStore(directory.resolve("ids.csv")); }
    public synchronized String nextId(String prefix) {
        var rows = store.readAll();
        long next = rows.isEmpty() ? 1 : Long.parseLong(rows.get(0)[0]);
        store.writeAll(Collections.singletonList(new String[]{Long.toString(next + 1)}));
        return prefix + next;
    }
}
