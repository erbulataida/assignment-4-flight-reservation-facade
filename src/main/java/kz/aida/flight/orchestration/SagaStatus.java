package kz.aida.flight.orchestration;

public enum SagaStatus {
    STARTED,
    SEAT_RESERVED,
    TICKET_ISSUED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED
}