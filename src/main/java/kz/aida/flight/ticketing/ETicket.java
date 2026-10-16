package kz.aida.flight.ticketing;

public class ETicket {

    private final String id;
    private final String reservationId;
    private final String passengerName;
    private final double baggageKg;

    private TicketStatus status;

    public ETicket(
            String id,
            String reservationId,
            String passengerName,
            double baggageKg
    ) {
        this(
                id,
                reservationId,
                passengerName,
                baggageKg,
                TicketStatus.NEW
        );
    }

    ETicket(
            String id,
            String reservationId,
            String passengerName,
            double baggageKg,
            TicketStatus status
    ) {
        this.id = id;
        this.reservationId = reservationId;
        this.passengerName = passengerName;
        this.baggageKg = baggageKg;
        this.status = status;
    }

    public void issue() {
        if (status != TicketStatus.NEW) {
            throw new IllegalStateException(
                    "Only a NEW ticket can be issued"
            );
        }

        status = TicketStatus.ISSUED;
    }

    public void voidTicket() {
        if (status != TicketStatus.ISSUED) {
            throw new IllegalStateException(
                    "Only an ISSUED ticket can be voided"
            );
        }

        status = TicketStatus.VOIDED;
    }

    public String getId() {
        return id;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public double getBaggageKg() {
        return baggageKg;
    }

    public TicketStatus getStatus() {
        return status;
    }
}