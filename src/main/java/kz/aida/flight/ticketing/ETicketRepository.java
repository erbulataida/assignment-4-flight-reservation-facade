package kz.aida.flight.ticketing;

import kz.aida.flight.shared.CsvStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ETicketRepository {

    private final CsvStore store;

    public ETicketRepository(Path dataDirectory) {
        Path file = dataDirectory.resolve("etickets.csv");
        this.store = new CsvStore(file);
    }

    public void save(ETicket ticket) {
        List<String[]> rows = store.readAll();
        List<String[]> updatedRows = new ArrayList<>();

        for (String[] row : rows) {
            if (!row[0].equals(ticket.getId())) {
                updatedRows.add(row);
            }
        }

        String[] ticketRow = {
                ticket.getId(),
                ticket.getReservationId(),
                ticket.getPassengerName(),
                Double.toString(ticket.getBaggageKg()),
                ticket.getStatus().name()
        };

        updatedRows.add(ticketRow);

        store.writeAll(updatedRows);
    }

    public List<ETicket> findAll() {
        List<String[]> rows = store.readAll();
        List<ETicket> tickets = new ArrayList<>();

        for (String[] row : rows) {
            if (row.length != 5) {
                throw new IllegalStateException(
                        "Invalid ticket record: expected 5 fields"
                );
            }

            ETicket ticket = new ETicket(
                    row[0],
                    row[1],
                    row[2],
                    Double.parseDouble(row[3]),
                    TicketStatus.valueOf(row[4])
            );

            tickets.add(ticket);
        }

        return tickets;
    }

    public Optional<ETicket> findById(String id) {
        for (ETicket ticket : findAll()) {
            if (ticket.getId().equals(id)) {
                return Optional.of(ticket);
            }
        }

        return Optional.empty();
    }

    public Optional<ETicket> findByReservationId(String reservationId) {
        for (ETicket ticket : findAll()) {
            if (ticket.getReservationId().equals(reservationId)) {
                return Optional.of(ticket);
            }
        }

        return Optional.empty();
    }
}