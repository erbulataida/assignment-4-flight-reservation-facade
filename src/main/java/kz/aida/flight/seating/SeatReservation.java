package kz.aida.flight.seating;

public class SeatReservation {

    private final String id;
    private final String flightNumber;
    private final String passengerName;

    private ReservationStatus status;

    public SeatReservation(
            String id,
            String flightNumber,
            String passengerName
    ) {
        this(
                id,
                flightNumber,
                passengerName,
                ReservationStatus.NEW
        );
    }

    SeatReservation(
            String id,
            String flightNumber,
            String passengerName,
            ReservationStatus status
    ) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.passengerName = passengerName;
        this.status = status;
    }

    public void reserve() {
        if (status != ReservationStatus.NEW) {
            throw new IllegalStateException(
                    "Only a NEW reservation can be reserved"
            );
        }

        status = ReservationStatus.RESERVED;
    }

    public void cancel() {
        if (status != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "Only a RESERVED reservation can be cancelled"
            );
        }

        status = ReservationStatus.CANCELLED;
    }

    public String getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}