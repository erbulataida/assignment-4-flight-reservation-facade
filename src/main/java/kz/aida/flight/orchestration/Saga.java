package kz.aida.flight.orchestration;

public class Saga {

    private final String id;
    private final String reservationId;
    private final String ticketId;
    private final String flightNumber;
    private final String passengerName;
    private final double baggageKg;

    private SagaStatus status;

    public Saga(
            String id,
            String reservationId,
            String ticketId,
            String flightNumber,
            String passengerName,
            double baggageKg
    ) {
        this(
                id,
                reservationId,
                ticketId,
                flightNumber,
                passengerName,
                baggageKg,
                SagaStatus.STARTED
        );
    }

    Saga(
            String id,
            String reservationId,
            String ticketId,
            String flightNumber,
            String passengerName,
            double baggageKg,
            SagaStatus status
    ) {
        this.id = id;
        this.reservationId = reservationId;
        this.ticketId = ticketId;
        this.flightNumber = flightNumber;
        this.passengerName = passengerName;
        this.baggageKg = baggageKg;
        this.status = status;
    }

    public void markSeatReserved() {
        if (status != SagaStatus.STARTED) {
            throw new IllegalStateException(
                    "Seat can be marked reserved only from STARTED"
            );
        }

        status = SagaStatus.SEAT_RESERVED;
    }

    public void markTicketIssued() {
        if (status != SagaStatus.SEAT_RESERVED) {
            throw new IllegalStateException(
                    "Ticket can be marked issued only from SEAT_RESERVED"
            );
        }

        status = SagaStatus.TICKET_ISSUED;
    }

    public void markCompleted() {
        if (status != SagaStatus.TICKET_ISSUED) {
            throw new IllegalStateException(
                    "Saga can be completed only from TICKET_ISSUED"
            );
        }

        status = SagaStatus.COMPLETED;
    }

    public void markCompensated() {
        if (status != SagaStatus.STARTED
                && status != SagaStatus.SEAT_RESERVED) {
            throw new IllegalStateException(
                    "Only STARTED or SEAT_RESERVED Saga can be compensated"
            );
        }

        status = SagaStatus.COMPENSATED;
    }

    public String getId() {
        return id;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public double getBaggageKg() {
        return baggageKg;
    }

    public SagaStatus getStatus() {
        return status;
    }
}