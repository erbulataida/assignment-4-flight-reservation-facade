package kz.aida.flight.seating;

import java.math.BigDecimal;

public class FareCalculator {

    public BigDecimal calculate(String seatNumber) {
        if (seatNumber == null || seatNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Seat number is required"
            );
        }

        if (seatNumber.startsWith("1")) {
            return new BigDecimal("80000.00");
        }

        return new BigDecimal("40000.00");
    }
}