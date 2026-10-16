package kz.aida.flight;

import kz.aida.flight.orchestration.FlightBookingFacade;
import kz.aida.flight.orchestration.FlightBookingResult;
import kz.aida.flight.orchestration.SagaRepository;
import kz.aida.flight.seating.SeatReservationRepository;
import kz.aida.flight.seating.SeatReservationService;
import kz.aida.flight.shared.IdGenerator;
import kz.aida.flight.ticketing.BaggageRulesEngine;
import kz.aida.flight.ticketing.ETicketRepository;
import kz.aida.flight.ticketing.TicketingService;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Path dataDirectory = Path.of("data");

        SeatReservationRepository seatReservationRepository =
                new SeatReservationRepository(dataDirectory);

        SeatReservationService seatReservationService =
                new SeatReservationService(seatReservationRepository);

        ETicketRepository ticketRepository =
                new ETicketRepository(dataDirectory);

        BaggageRulesEngine baggageRulesEngine =
                new BaggageRulesEngine();

        TicketingService ticketingService =
                new TicketingService(ticketRepository, baggageRulesEngine);

        SagaRepository sagaRepository =
                new SagaRepository(dataDirectory);

        IdGenerator idGenerator =
                new IdGenerator(dataDirectory);

        FlightBookingFacade facade = new FlightBookingFacade(
                seatReservationService,
                ticketingService,
                sagaRepository,
                idGenerator
        );

        facade.recoverPendingSagas();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Flight number:");
        String flightNumber = scanner.nextLine();

        System.out.println("Passenger name:");
        String passengerName = scanner.nextLine();

        System.out.println("Baggage weight in kg:");
        double baggageKg = Double.parseDouble(scanner.nextLine());

        FlightBookingResult result = facade.bookFlight(
                flightNumber,
                passengerName,
                baggageKg
        );

        if (result.isCompleted()) {
            System.out.println("Booking completed");
            System.out.println("Saga ID: " + result.getSagaId());
            System.out.println("Reservation ID: " + result.getReservationId());
            System.out.println("Ticket ID: " + result.getTicketId());
        } else {
            System.out.println("Booking failed");
            System.out.println("Saga ID: " + result.getSagaId());
            System.out.println("Reason: " + result.getErrorMessage());
            System.out.println("Seat reservation was compensated");
        }
    }
}