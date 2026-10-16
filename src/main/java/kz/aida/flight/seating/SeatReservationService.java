package kz.aida.flight.seating;

import java.util.List;
import java.util.Optional;

public class SeatReservationService {

    private final SeatReservationRepository reservationRepository;

    public SeatReservationService(SeatReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public SeatReservation reserveSeat(
            String reservationId,
            String flightNumber,
            String passengerName
    ) {
        kz.aida.flight.shared.CsvStore.validateField(flightNumber);
        kz.aida.flight.shared.CsvStore.validateField(passengerName);
        if (passengerName.isBlank()) throw new IllegalArgumentException("Passenger name is required");
        if (reservationRepository.findById(reservationId).isPresent()) throw new IllegalArgumentException("Duplicate reservation ID");
        String seat = new SeatMap().availableSeat(flightNumber, reservationRepository.findAll());
        SeatReservation reservation = new SeatReservation(
                reservationId,
                flightNumber,
                passengerName
        );

        reservation.assignSeat(seat);
        reservation.reserve();
        reservationRepository.save(reservation);

        return reservation;
    }

    public void cancelReservation(String reservationId) {
        SeatReservation reservation = reservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reservation not found: " + reservationId
                ));

        reservation.cancel();
        reservationRepository.save(reservation);
    }

    public List<SeatReservation> getReservations() {
        return reservationRepository.findAll();
    }

    public Optional<SeatReservation> getReservationById(String reservationId) {
        return reservationRepository.findById(reservationId);
    }
}