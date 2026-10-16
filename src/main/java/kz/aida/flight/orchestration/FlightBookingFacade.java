package kz.aida.flight.orchestration;

import kz.aida.flight.seating.SeatReservation;
import kz.aida.flight.seating.SeatReservationService;
import kz.aida.flight.shared.IdGenerator;
import kz.aida.flight.ticketing.ETicket;
import kz.aida.flight.ticketing.TicketingService;
import kz.aida.flight.ticketing.TicketIssuanceException;

import java.nio.file.Path;

public class FlightBookingFacade {

    private final SeatReservationService seatReservationService;
    private final TicketingService ticketingService;
    private final SagaRepository sagaRepository;
    private final IdGenerator idGenerator;

    public FlightBookingFacade(
            SeatReservationService seatReservationService,
            TicketingService ticketingService,
            SagaRepository sagaRepository,
            IdGenerator idGenerator
    ) {
        this.seatReservationService = seatReservationService;
        this.ticketingService = ticketingService;
        this.sagaRepository = sagaRepository;
        this.idGenerator = idGenerator;
    }

    public FlightBookingResult bookFlight(
            String flightNumber,
            String passengerName,
            double baggageKg
    ) {
        String sagaId = idGenerator.nextId("S");
        String reservationId = idGenerator.nextId("R");
        String ticketId = idGenerator.nextId("T");

        Saga saga = new Saga(
                sagaId,
                reservationId,
                ticketId,
                flightNumber,
                passengerName,
                baggageKg,
                SagaStatus.STARTED
        );

        sagaRepository.save(saga);

        try {
            seatReservationService.reserveSeat(
                    reservationId,
                    flightNumber,
                    passengerName
            );

            saga.markSeatReserved();
            sagaRepository.save(saga);

            ETicket ticket = ticketingService.issueTicket(
                    ticketId,
                    reservationId,
                    passengerName,
                    baggageKg
            );

            saga.markTicketIssued();
            sagaRepository.save(saga);

            return FlightBookingResult.completed(
                    sagaId,
                    reservationId,
                    ticketId
            );

        } catch (TicketIssuanceException exception) {
            compensate(saga, exception.getMessage());

            return FlightBookingResult.failed(
                    sagaId,
                    exception.getMessage()
            );
        }
    }

    public void recoverPendingSagas() {
        for (Saga saga : sagaRepository.findAll()) {
            if (saga.getStatus() != SagaStatus.STARTED) {
                continue;
            }

            if (isSeatReservationSaved(saga)) {
                saga.markSeatReserved();
                sagaRepository.save(saga);
            }

            if (isTicketSaved(saga)) {
                saga.markTicketIssued();
                sagaRepository.save(saga);
                continue;
            }

            if (saga.getStatus() == SagaStatus.SEAT_RESERVED) {
                try {
                    ticketingService.issueTicket(
                            saga.getTicketId(),
                            saga.getReservationId(),
                            saga.getPassengerName(),
                            saga.getBaggageKg()
                    );

                    saga.markTicketIssued();
                    sagaRepository.save(saga);

                } catch (TicketIssuanceException exception) {
                    compensate(saga, exception.getMessage());
                }
            }
        }
    }

    private void compensate(Saga saga, String reason) {
        if (isSeatReservationSaved(saga)) {
            seatReservationService.cancelReservation(
                    saga.getReservationId()
            );
        }

        saga.markCompensated();
        sagaRepository.save(saga);
    }

    private boolean isSeatReservationSaved(Saga saga) {
        return seatReservationService
                .getReservationById(saga.getReservationId())
                .isPresent();
    }

    private boolean isTicketSaved(Saga saga) {
        return ticketingService
                .getTicketByReservationId(saga.getReservationId())
                .isPresent();
    }
}