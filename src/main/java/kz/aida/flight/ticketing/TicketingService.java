package kz.aida.flight.ticketing;

import java.util.List;
import java.util.Optional;

public class TicketingService {

    private final ETicketRepository ticketRepository;
    private final BaggageRulesEngine baggageRulesEngine;

    public TicketingService(
            ETicketRepository ticketRepository,
            BaggageRulesEngine baggageRulesEngine
    ) {
        this.ticketRepository = ticketRepository;
        this.baggageRulesEngine = baggageRulesEngine;
    }

    public ETicket issueTicket(
            String ticketId,
            String reservationId,
            String passengerName,
            double baggageKg
    ) {
        if (ticketRepository.findById(ticketId).isPresent()) throw new IllegalArgumentException("Duplicate ticket ID");
        baggageRulesEngine.validate(baggageKg);

        ETicket ticket = new ETicket(
                ticketId,
                reservationId,
                passengerName,
                baggageKg
        );

        ticket.issue();
        ticketRepository.save(ticket);

        return ticket;
    }

    public void voidTicket(String ticketId) {
        ETicket ticket = ticketRepository
                .findById(ticketId)
                .orElseThrow(() -> new IllegalStateException(
                        "Ticket not found: " + ticketId
                ));

        ticket.voidTicket();
        ticketRepository.save(ticket);
    }

    public List<ETicket> getTickets() {
        return ticketRepository.findAll();
    }

    public Optional<ETicket> getTicketByReservationId(String reservationId) {
        return ticketRepository.findByReservationId(reservationId);
    }
}