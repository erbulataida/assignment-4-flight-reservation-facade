package kz.aida.flight.seating;
import java.util.List;
public class SeatMap {
    public String availableSeat(String flight, List<SeatReservation> reservations) {
        if (flight == null || flight.isBlank()) throw new IllegalArgumentException("Flight is required");
        for (String seat : List.of("1A", "2A", "2B")) {
            boolean occupied = reservations.stream().anyMatch(r -> r.getFlightNumber().equals(flight)
                    && r.getSeatNumber().equals(seat) && r.getStatus() == ReservationStatus.RESERVED);
            if (!occupied) return seat;
        }
        throw new IllegalArgumentException("Flight is full (demo aircraft: 3 seats)");
    }
}
