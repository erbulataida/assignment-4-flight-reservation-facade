package kz.aida.flight.seating;

import kz.aida.flight.shared.CsvStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeatReservationRepository {

    private final CsvStore store;

    public SeatReservationRepository(Path dataDirectory) {
        Path file = dataDirectory.resolve("seat-reservations.csv");
        this.store = new CsvStore(file);
    }

    public void save(SeatReservation reservation) {
        List<String[]> rows = store.readAll();
        List<String[]> updatedRows = new ArrayList<>();

        for (String[] row : rows) {
            if (!row[0].equals(reservation.getId())) {
                updatedRows.add(row);
            }
        }

        String[] reservationRow = {
                reservation.getId(),
                reservation.getFlightNumber(),
                reservation.getPassengerName(),
                reservation.getStatus().name(),
                reservation.getSeatNumber()
        };

        updatedRows.add(reservationRow);

        store.writeAll(updatedRows);
    }

    public List<SeatReservation> findAll() {
        List<String[]> rows = store.readAll();
        List<SeatReservation> reservations = new ArrayList<>();

        for (String[] row : rows) {
            if (row.length != 4 && row.length != 5) {
                throw new IllegalStateException(
                        "Invalid reservation record: expected 4 fields"
                );
            }

            SeatReservation reservation = new SeatReservation(
                    row[0],
                    row[1],
                    row[2],
                    ReservationStatus.valueOf(row[3])
            );

            if (row.length == 5) reservation.restoreSeat(row[4]);
            reservations.add(reservation);
        }

        return reservations;
    }

    public Optional<SeatReservation> findById(String id) {
        for (SeatReservation reservation : findAll()) {
            if (reservation.getId().equals(id)) {
                return Optional.of(reservation);
            }
        }

        return Optional.empty();
    }
}