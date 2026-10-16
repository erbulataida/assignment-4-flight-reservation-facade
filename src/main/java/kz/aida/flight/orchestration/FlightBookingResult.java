package kz.aida.flight.orchestration;

public class FlightBookingResult {

    private final String sagaId;
    private final String reservationId;
    private final String ticketId;
    private final boolean completed;
    private final String errorMessage;

    private FlightBookingResult(
            String sagaId,
            String reservationId,
            String ticketId,
            boolean completed,
            String errorMessage
    ) {
        this.sagaId = sagaId;
        this.reservationId = reservationId;
        this.ticketId = ticketId;
        this.completed = completed;
        this.errorMessage = errorMessage;
    }

    public static FlightBookingResult completed(
            String sagaId,
            String reservationId,
            String ticketId
    ) {
        return new FlightBookingResult(
                sagaId,
                reservationId,
                ticketId,
                true,
                null
        );
    }

    public static FlightBookingResult failed(
            String sagaId,
            String errorMessage
    ) {
        return new FlightBookingResult(
                sagaId,
                null,
                null,
                false,
                errorMessage
        );
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}