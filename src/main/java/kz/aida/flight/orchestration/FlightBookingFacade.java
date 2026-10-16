package kz.aida.flight.orchestration;
import kz.aida.flight.seating.*;
import kz.aida.flight.ticketing.*;
import kz.aida.flight.preferences.*;
import kz.aida.flight.shared.*;
import java.nio.file.Path;

/** Coordinates independent services. All recovery decisions consult disk, not process-local flags. */
public class FlightBookingFacade {
    private final SeatReservationService seats;
    private final TicketingService tickets;
    private final LoyaltyMealService preferences;
    private final SagaRepository sagas;
    private final IdGenerator ids;
    public FlightBookingFacade(SeatReservationService seats, TicketingService tickets,
            LoyaltyMealService preferences, SagaRepository sagas, IdGenerator ids) {
        this.seats=seats; this.tickets=tickets; this.preferences=preferences; this.sagas=sagas; this.ids=ids;
    }
    public static FlightBookingFacade open(Path dir) {
        return new FlightBookingFacade(new SeatReservationService(new SeatReservationRepository(dir)),
            new TicketingService(new ETicketRepository(dir),new BaggageRulesEngine()),
            new LoyaltyMealService(new PreferenceRepository(dir)),new SagaRepository(dir),new IdGenerator(dir));
    }
    public FlightBookingResult bookFlight(String flight, String passenger, double baggage) {
        return bookFlight(flight, passenger, baggage, "STANDARD");
    }
    public FlightBookingResult bookFlight(String flight, String passenger, double baggage, String meal) {
        String id=startBooking(flight,passenger,baggage,meal);
        return continueBooking(id, 3);
    }
    /** Durable request, used by the live restart demonstration. */
    public String startBooking(String flight, String passenger, double baggage, String meal) {
        CsvStore.validateField(flight); CsvStore.validateField(passenger); CsvStore.validateField(meal);
        if (flight.isBlank() || passenger.isBlank() || meal.isBlank()) throw new IllegalArgumentException("All text fields are required");
        if (!Double.isFinite(baggage) || baggage < 0) throw new IllegalArgumentException("Invalid baggage weight");
        Saga saga=new Saga(ids.nextId("S"),ids.nextId("R"),ids.nextId("T"),flight,passenger,baggage);
        saga.setMeal(meal.toUpperCase(java.util.Locale.ROOT)); sagas.save(saga); return saga.getId();
    }
    /** stopAfter is a demo checkpoint: 1=A, 2=B, 3=all. It never fabricates an exception. */
    public FlightBookingResult continueBooking(String id, int stopAfter) {
        if (stopAfter < 1 || stopAfter > 3) throw new IllegalArgumentException("Checkpoint must be 1..3");
        Saga saga=sagas.findById(id).orElseThrow();
        if (saga.getStatus()==SagaStatus.COMPENSATING) { compensate(id); return FlightBookingResult.failed(id,"Compensated after restart"); }
        if (saga.getStatus()==SagaStatus.COMPENSATED) return FlightBookingResult.failed(id,"Already compensated");
        if (saga.getStatus()==SagaStatus.COMPLETED) return completed(saga);
        try {
            var seat=seats.getReservationById(saga.getReservationId());
            if (seat.isEmpty()) seats.reserveSeat(saga.getReservationId(),saga.getFlightNumber(),saga.getPassengerName());
            else if (seat.get().getStatus()!=ReservationStatus.RESERVED) throw new IllegalArgumentException("Seat is no longer reserved");
            if (saga.getStatus()==SagaStatus.STARTED) { saga.markSeatReserved(); sagas.save(saga); }
            if (stopAfter==1) return FlightBookingResult.failed(id,"Paused after A; restart to recover");
            var ticket=tickets.getTicketByReservationId(saga.getReservationId());
            if (ticket.isEmpty()) tickets.issueTicket(saga.getTicketId(),saga.getReservationId(),saga.getPassengerName(),saga.getBaggageKg());
            else if (ticket.get().getStatus()!=TicketStatus.ISSUED) throw new IllegalArgumentException("Ticket is no longer issued");
            if (saga.getStatus()==SagaStatus.SEAT_RESERVED) { saga.markTicketIssued(); sagas.save(saga); }
            if (stopAfter==2) return FlightBookingResult.failed(id,"Paused after B; restart to recover");
            var preference=preferences.find(id);
            if (preference.isEmpty()) preferences.confirm(id,saga.getPassengerName(),saga.getMeal());
            else if (preference.get().getStatus()!=PreferenceStatus.CONFIRMED) throw new IllegalArgumentException("Preferences cancelled");
            saga.markCompleted(); sagas.save(saga); return completed(saga);
        } catch (IllegalArgumentException | TicketIssuanceException e) {
            compensate(id); return FlightBookingResult.failed(id,e.getMessage());
        }
        // Storage failures propagate. Pending/COMPENSATING status remains retryable on restart.
    }
    private FlightBookingResult completed(Saga s) { return FlightBookingResult.completed(s.getId(),s.getReservationId(),s.getTicketId()); }
    public void recoverPendingSagas() {
        for (Saga s:sagas.findAll()) if (s.getStatus()!=SagaStatus.COMPLETED && s.getStatus()!=SagaStatus.COMPENSATED)
            continueBooking(s.getId(),3);
    }
    /** Idempotent compensation: persist intent first; read statuses before each reverse action. */
    public void compensate(String id) {
        Saga saga=sagas.findById(id).orElseThrow();
        if (saga.getStatus()==SagaStatus.COMPENSATED) return;
        saga.markCompensating(); sagas.save(saga);
        preferences.find(id).filter(p->p.getStatus()==PreferenceStatus.CONFIRMED).ifPresent(p->preferences.cancel(id));
        tickets.getTicketByReservationId(saga.getReservationId()).filter(t->t.getStatus()==TicketStatus.ISSUED)
                .ifPresent(t->tickets.voidTicket(t.getId()));
        seats.getReservationById(saga.getReservationId()).filter(r->r.getStatus()==ReservationStatus.RESERVED)
                .ifPresent(r->seats.cancelReservation(r.getId()));
        saga.markCompensated(); sagas.save(saga);
    }
}
