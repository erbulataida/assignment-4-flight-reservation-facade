package kz.aida.flight.ticketing;

public class BaggageRulesEngine {

    private static final double MAX_BAGGAGE_KG = 30.0;

    public void validate(double baggageKg) {
        if (!Double.isFinite(baggageKg) || baggageKg < 0) {
            throw new TicketIssuanceException(
                    "Baggage weight must be a finite non-negative number"
            );
        }

        if (baggageKg > MAX_BAGGAGE_KG) {
            throw new TicketIssuanceException(
                    "Baggage exceeds the allowed limit of "
                            + MAX_BAGGAGE_KG
                            + " kg"
            );
        }
    }
}