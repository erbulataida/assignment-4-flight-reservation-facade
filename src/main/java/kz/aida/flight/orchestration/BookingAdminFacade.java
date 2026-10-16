package kz.aida.flight.orchestration;
import java.nio.file.Path;
import kz.aida.flight.seating.*;
import kz.aida.flight.ticketing.*;
import kz.aida.flight.preferences.*;
public class BookingAdminFacade {
    private final Path directory;
    private final FlightBookingFacade booking;
    public BookingAdminFacade(Path directory, FlightBookingFacade booking) { this.directory=directory; this.booking=booking; }
    public void compensate(String id) { booking.compensate(id); }
    public String inspect() {
        StringBuilder out=new StringBuilder("SAGAS\n");
        for (var s:new SagaRepository(directory).findAll()) out.append(s.getId()).append(" ").append(s.getStatus()).append(" ").append(s.getPassengerName()).append("\n");
        out.append("SEATS\n");
        for (var r:new SeatReservationRepository(directory).findAll()) out.append(r.getId()).append(" ").append(r.getSeatNumber()).append(" fare=").append(r.getFare()).append(" ").append(r.getStatus()).append("\n");
        out.append("TICKETS\n");
        for (var t:new ETicketRepository(directory).findAll()) out.append(t.getId()).append(" ").append(t.getStatus()).append("\n");
        out.append("PREFERENCES\n");
        for (var p:new PreferenceRepository(directory).findAll()) out.append(p.getId()).append(" ").append(p.getMeal()).append(" points=").append(p.getPoints()).append(" ").append(p.getStatus()).append("\n");
        return out.toString();
    }
}
