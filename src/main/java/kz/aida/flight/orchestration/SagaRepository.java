package kz.aida.flight.orchestration;

import kz.aida.flight.shared.CsvStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SagaRepository {

    private final CsvStore store;

    public SagaRepository(Path dataDirectory) {
        Path file = dataDirectory.resolve("sagas.csv");
        this.store = new CsvStore(file);
    }

    public void save(Saga saga) {
        List<String[]> rows = store.readAll();
        List<String[]> updatedRows = new ArrayList<>();

        for (String[] row : rows) {
            if (!row[0].equals(saga.getId())) {
                updatedRows.add(row);
            }
        }

        String[] sagaRow = {
                saga.getId(),
                saga.getReservationId(),
                saga.getTicketId(),
                saga.getFlightNumber(),
                saga.getPassengerName(),
                Double.toString(saga.getBaggageKg()),
                saga.getStatus().name()
        };

        updatedRows.add(sagaRow);

        store.writeAll(updatedRows);
    }

    public List<Saga> findAll() {
        List<String[]> rows = store.readAll();
        List<Saga> sagas = new ArrayList<>();

        for (String[] row : rows) {
            if (row.length != 7) {
                throw new IllegalStateException(
                        "Invalid Saga record: expected 7 fields"
                );
            }

            Saga saga = new Saga(
                    row[0],
                    row[1],
                    row[2],
                    row[3],
                    row[4],
                    Double.parseDouble(row[5]),
                    SagaStatus.valueOf(row[6])
            );

            sagas.add(saga);
        }

        return sagas;
    }

    public Optional<Saga> findById(String id) {
        for (Saga saga : findAll()) {
            if (saga.getId().equals(id)) {
                return Optional.of(saga);
            }
        }

        return Optional.empty();
    }
}