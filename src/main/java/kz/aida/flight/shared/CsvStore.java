package kz.aida.flight.shared;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class CsvStore {
    private final Path file;
    public CsvStore(Path file) { this.file = file.toAbsolutePath(); }
    public List<String> readLines() {
        try { return Files.exists(file) ? Files.readAllLines(file) : new ArrayList<>(); }
        catch (IOException e) { throw new UncheckedIOException("Cannot read " + file, e); }
    }
    public List<String[]> readAll() {
        List<String[]> rows = new ArrayList<>();
        for (String line : readLines()) if (!line.isBlank()) rows.add(line.split("\\|", -1));
        return rows;
    }
    public void writeAll(List<String[]> rows) {
        List<String> lines = new ArrayList<>();
        for (String[] row : rows) {
            for (String field : row) validateField(field);
            lines.add(String.join("|", row));
        }
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "store-", ".tmp");
            Files.write(temp, lines);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new UncheckedIOException("Cannot write " + file, e); }
        finally { if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { } }
    }
    public static void validateField(String value) {
        if (value == null || value.contains("|") || value.contains("\n") || value.contains("\r"))
            throw new IllegalArgumentException("Text cannot contain pipes or newlines");
    }
}
