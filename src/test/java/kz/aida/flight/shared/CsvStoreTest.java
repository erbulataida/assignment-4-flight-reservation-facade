package kz.aida.flight.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndReadsOneReservation() {
        Path file = tempDir.resolve("reservations.csv");
        CsvStore store = new CsvStore(file);

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"R1", "Aida", "1A", "RESERVED"});

        store.writeAll(rows);

        CsvStore reopenedStore = new CsvStore(file);
        List<String[]> savedRows = reopenedStore.readAll();

        assertEquals(1, savedRows.size());

        assertArrayEquals(
                new String[]{"R1", "Aida", "1A", "RESERVED"},
                savedRows.get(0)
        );
    }
    @Test
    void replacesOldRecordsWhenWritingAgain() {
        Path file = tempDir.resolve("reservations.csv");
        CsvStore store = new CsvStore(file);

        List<String[]> originalRows = new ArrayList<>();
        originalRows.add(new String[]{"R1", "Aida", "1A", "RESERVED"});
        originalRows.add(new String[]{"R2", "Dana", "2B", "RESERVED"});

        store.writeAll(originalRows);

        List<String[]> replacementRows = new ArrayList<>();
        replacementRows.add(
                new String[]{"R3", "Aliya", "3C", "RESERVED"}
        );

        store.writeAll(replacementRows);

        List<String[]> savedRows = store.readAll();

        assertEquals(1, savedRows.size());

        assertArrayEquals(
                new String[]{"R3", "Aliya", "3C", "RESERVED"},
                savedRows.get(0)
        );
    }
}