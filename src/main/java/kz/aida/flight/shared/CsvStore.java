package kz.aida.flight.shared;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

public class CsvStore {

    private final Path file;

    public CsvStore(Path file) {
        this.file = file;
    }
    public List<String> readLines() {
        if (Files.notExists(file)) {
            return new ArrayList<>();
        }

        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Cannot read file: " + file, e
            );
        }
    }





    public List<String[]> readAll() {
        List<String[]> rows = new ArrayList<>();

        for (String line : readLines()) {
            if (!line.isBlank()) {
                String[] fields = line.split("\\|", -1);
                rows.add(fields);
            }
        }

        return rows;
    }






    public void writeAll(List<String[]> rows) {
        List<String> lines = new ArrayList<>();

        for (String[] row : rows) {
            String line = String.join("|", row);
            lines.add(line);
        }

        try {
            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Cannot write file: " + file, e
            );
        }
    }
}