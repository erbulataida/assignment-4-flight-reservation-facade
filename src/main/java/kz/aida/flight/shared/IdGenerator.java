package kz.aida.flight.shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class IdGenerator {

    private final Path counterFile;

    public IdGenerator(Path dataDirectory) {
        this.counterFile = dataDirectory.resolve("ids.csv");
    }

    public synchronized String nextId(String prefix) {
        long nextNumber = readNextNumber();

        writeNextNumber(nextNumber + 1);

        return prefix + nextNumber;
    }

    private long readNextNumber() {
        if (!Files.exists(counterFile)) {
            return 1;
        }

        try {
            List<String> lines = Files.readAllLines(counterFile, StandardCharsets.UTF_8);

            if (lines.isEmpty() || lines.get(0).isBlank()) {
                return 1;
            }

            return Long.parseLong(lines.get(0));
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException(
                    "Cannot read identifier counter: " + counterFile,
                    e
            );
        }
    }

    private void writeNextNumber(long nextNumber) {
        try {
            Files.createDirectories(counterFile.getParent());
            Files.write(
                    counterFile,
                    List.of(Long.toString(nextNumber)),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot write identifier counter: " + counterFile,
                    e
            );
        }
    }
}