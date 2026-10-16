package kz.aida.flight.orchestration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import kz.aida.flight.seating.*;
import kz.aida.flight.ticketing.*;
import kz.aida.flight.preferences.*;
import kz.aida.flight.shared.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingTest {
    @TempDir Path dir;
    FlightBookingFacade booking() { return FlightBookingFacade.open(dir); }
    // Domain-specific testing language. Each test owns its persisted directory.
    FlightBookingResult aPassengerBooksWith(double kg, String meal) {
        return booking().bookFlight("KC123", "Aida", kg, meal);
    }
    Saga saga(String id) { return new SagaRepository(dir).findById(id).orElseThrow(); }
    SeatReservation seat(Saga s) { return new SeatReservationRepository(dir).findById(s.getReservationId()).orElseThrow(); }
    ETicket ticket(Saga s) { return new ETicketRepository(dir).findById(s.getTicketId()).orElseThrow(); }
    void assertCompensated(String id, boolean ticketExisted) {
        var s=saga(id);
        assertEquals(SagaStatus.COMPENSATED,s.getStatus());
        assertEquals(ReservationStatus.CANCELLED,seat(s).getStatus());
        if (ticketExisted) assertEquals(TicketStatus.VOIDED,ticket(s).getStatus());
        else assertTrue(new ETicketRepository(dir).findAll().isEmpty());
        assertTrue(new PreferenceRepository(dir).findAll().isEmpty());
    }
    @Test void booksAllThreeStagesAndReloadsThem() {
        var result=aPassengerBooksWith(20,"HALAL"); assertTrue(result.isCompleted());
        assertNull(result.getErrorMessage());
        var s=saga(result.getSagaId()); assertEquals(SagaStatus.COMPLETED,s.getStatus());
        assertEquals(result.getReservationId(),seat(s).getId());
        assertEquals(result.getTicketId(),ticket(s).getId());
        assertEquals("Aida",ticket(s).getPassengerName()); assertEquals(20,ticket(s).getBaggageKg());
        assertEquals("Aida",seat(s).getPassengerName()); assertEquals("KC123",seat(s).getFlightNumber());
        var p=new PreferenceRepository(dir).findById(s.getId()).orElseThrow();
        assertEquals("Aida",p.getPassenger()); assertEquals(100,p.getPoints());
        booking().recoverPendingSagas(); assertTrue(booking().continueBooking(s.getId(),3).isCompleted());
        assertEquals(1,new ETicketRepository(dir).findAll().size());
        assertTrue(new BookingAdminFacade(dir,booking()).inspect().contains("HALAL"));
    }
    @Test void baggageFailureCompensatesOnlyA() {
        var result=aPassengerBooksWith(35,"STANDARD"); assertFalse(result.isCompleted());
        assertTrue(result.getErrorMessage().contains("Baggage")); assertCompensated(result.getSagaId(),false);
    }
    @Test void mealFailureCompensatesBThenAInThatOrder() {
        List<String> calls=new ArrayList<>();
        var seats=new SeatReservationService(new SeatReservationRepository(dir)) {
            @Override public void cancelReservation(String id) { calls.add("A"); super.cancelReservation(id); }
        };
        var tickets=new TicketingService(new ETicketRepository(dir),new BaggageRulesEngine()) {
            @Override public void voidTicket(String id) { calls.add("B"); super.voidTicket(id); }
        };
        var facade=new FlightBookingFacade(seats,tickets,new LoyaltyMealService(new PreferenceRepository(dir)),new SagaRepository(dir),new IdGenerator(dir));
        var result=facade.bookFlight("KC123","Aida",20,"KOSHER");
        assertFalse(result.isCompleted()); assertEquals(List.of("B","A"),calls);
        assertCompensated(result.getSagaId(),true);
    }
    @Test void restartAfterAResumesAndDoesNotAllocateAnotherSeat() {
        var f=booking(); String id=f.startBooking("KC1","Aida",20,"STANDARD");
        f.continueBooking(id,1); assertEquals(SagaStatus.SEAT_RESERVED,saga(id).getStatus());
        booking().recoverPendingSagas(); assertEquals(SagaStatus.COMPLETED,saga(id).getStatus());
        assertEquals(1,new SeatReservationRepository(dir).findAll().size());
    }
    @Test void restartAfterBResumesAndCanFailC() {
        var f=booking(); String id=f.startBooking("KC1","Aida",20,"KOSHER"); f.continueBooking(id,2);
        assertEquals(SagaStatus.TICKET_ISSUED,saga(id).getStatus()); booking().recoverPendingSagas(); assertCompensated(id,true);
    }
    @Test void restartReadsACommittedBeforeProgressWasSaved() {
        String id=booking().startBooking("KC1","Aida",20,"STANDARD"); var s=saga(id);
        new SeatReservationService(new SeatReservationRepository(dir)).reserveSeat(s.getReservationId(),s.getFlightNumber(),s.getPassengerName());
        booking().recoverPendingSagas(); assertEquals(SagaStatus.COMPLETED,saga(id).getStatus());
    }
    @Test void restartReadsBAndCCommittedBeforeProgressWasSaved() {
        var f=booking(); String id=f.startBooking("KC1","Aida",20,"HALAL"); f.continueBooking(id,1); var s=saga(id);
        new TicketingService(new ETicketRepository(dir),new BaggageRulesEngine()).issueTicket(s.getTicketId(),s.getReservationId(),s.getPassengerName(),20);
        new LoyaltyMealService(new PreferenceRepository(dir)).confirm(id,"Aida","HALAL");
        booking().recoverPendingSagas(); assertEquals(SagaStatus.COMPLETED,saga(id).getStatus());
        assertEquals(1,new PreferenceRepository(dir).findAll().size());
    }
    @Test void restartWithoutAnyStageStartsA() {
        String id=booking().startBooking("KC1","Aida",0,"STANDARD"); booking().recoverPendingSagas();
        assertEquals(SagaStatus.COMPLETED,saga(id).getStatus());
    }
    @Test void restartDuringCompensationFinishesRemainingA() {
        var f=booking(); String id=f.startBooking("KC1","Aida",20,"STANDARD"); f.continueBooking(id,2);
        var s=saga(id); s.markCompensating(); new SagaRepository(dir).save(s);
        new TicketingService(new ETicketRepository(dir),new BaggageRulesEngine()).voidTicket(s.getTicketId());
        booking().recoverPendingSagas(); assertCompensated(id,true);
        booking().compensate(id); assertFalse(booking().continueBooking(id,3).isCompleted());
    }
    @Test void adminCancelsCompletedBookingAndReversesLoyalty() {
        var r=aPassengerBooksWith(0,"VEGETARIAN"); var admin=new BookingAdminFacade(dir,booking());
        admin.compensate(r.getSagaId()); admin.compensate(r.getSagaId());
        assertEquals(0,new PreferenceRepository(dir).findById(r.getSagaId()).orElseThrow().getPoints());
        assertTrue(admin.inspect().contains("VOIDED"));
        assertEquals(PreferenceStatus.CANCELLED,new PreferenceRepository(dir).findAll().get(0).getStatus());
    }
    @Test void fullFlightFailsAtABeforeTicketingAndCancelledSeatIsReusable() {
        var a=aPassengerBooksWith(0,"STANDARD"); aPassengerBooksWith(0,"STANDARD"); aPassengerBooksWith(0,"STANDARD");
        var failure=aPassengerBooksWith(0,"STANDARD"); assertFalse(failure.isCompleted());
        assertTrue(failure.getErrorMessage().contains("full")); assertEquals(3,new ETicketRepository(dir).findAll().size());
        booking().compensate(a.getSagaId()); assertTrue(aPassengerBooksWith(0,"STANDARD").isCompleted());
    }
    @Test void invalidInputDoesNotCommitAnything() {
        for(double kg:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->aPassengerBooksWith(kg,"STANDARD"));
        assertThrows(IllegalArgumentException.class,()->booking().bookFlight("","Aida",0,"STANDARD"));
        assertThrows(IllegalArgumentException.class,()->booking().bookFlight("KC","A|da",0,"STANDARD"));
        assertTrue(new SagaRepository(dir).findAll().isEmpty());
        assertThrows(IllegalArgumentException.class,()->booking().continueBooking("none",0));
    }
    @Test void rejectsCancelledResourcesOnRecovery() {
        String id=booking().startBooking("KC","Aida",0,"STANDARD"); booking().continueBooking(id,1);
        var s=saga(id); new SeatReservationService(new SeatReservationRepository(dir)).cancelReservation(s.getReservationId());
        assertFalse(booking().continueBooking(id,3).isCompleted());
        String second=booking().startBooking("KC","Aida",0,"STANDARD"); booking().continueBooking(second,2);
        new TicketingService(new ETicketRepository(dir),new BaggageRulesEngine()).voidTicket(saga(second).getTicketId());
        assertFalse(booking().continueBooking(second,3).isCompleted());
    }
    @Test void eachSubsystemWorksDirectlyAndEnforcesItsOwnTransitions() {
        var seats=new SeatReservationService(new SeatReservationRepository(dir));
        var r=seats.reserveSeat("R1","KC","Dana"); assertEquals(1,seats.getReservations().size());
        assertThrows(IllegalStateException.class,r::reserve); assertThrows(IllegalStateException.class,()->r.assignSeat("2B"));
        assertThrows(IllegalArgumentException.class,()->seats.reserveSeat("R1","KC","Dana"));
        seats.cancelReservation("R1"); assertThrows(IllegalStateException.class,()->seats.cancelReservation("R1"));
        assertThrows(IllegalStateException.class,()->seats.cancelReservation("missing"));
        assertThrows(IllegalArgumentException.class,()->seats.reserveSeat("R2","KC",""));
        assertThrows(IllegalArgumentException.class,()->new SeatMap().availableSeat("",List.of()));
        assertThrows(IllegalArgumentException.class,()->new FareCalculator().calculate(null));
        assertThrows(IllegalArgumentException.class,()->new FareCalculator().calculate(""));
        assertEquals("40000.00",new FareCalculator().calculate("2B").toString());
        var tickets=new TicketingService(new ETicketRepository(dir),new BaggageRulesEngine());
        var t=tickets.issueTicket("T1","independent","Dana",30); assertEquals(1,tickets.getTickets().size());
        assertThrows(IllegalStateException.class,t::issue);
        assertThrows(IllegalArgumentException.class,()->tickets.issueTicket("T1","x","Dana",0));
        tickets.voidTicket("T1"); assertThrows(IllegalStateException.class,()->tickets.voidTicket("T1"));
        assertThrows(IllegalStateException.class,()->tickets.voidTicket("missing"));
        assertThrows(TicketIssuanceException.class,()->new BaggageRulesEngine().validate(Double.NaN));
        var service=new LoyaltyMealService(new PreferenceRepository(dir));
        var p=service.confirm("P1","Dana","STANDARD"); assertEquals(1,service.all().size());
        assertThrows(IllegalStateException.class,p::confirm);
        assertThrows(IllegalArgumentException.class,()->service.confirm("P1","Dana","STANDARD"));
        service.cancel("P1"); assertThrows(IllegalStateException.class,()->service.cancel("P1"));
        assertTrue(service.find("missing").isEmpty());
        assertThrows(IllegalStateException.class,()->new SeatReservation("x","KC","Dana").cancel());
        assertThrows(IllegalStateException.class,()->new ETicket("x","r","Dana",0).voidTicket());
        assertThrows(IllegalStateException.class,()->new TravelPreference("x","Dana","HALAL",100).cancel());
    }
    @Test void sagaRejectsIllegalForwardTransitions() {
        var s=new Saga("S","R","T","KC","Aida",0);
        assertThrows(IllegalStateException.class,s::markCompleted); assertThrows(IllegalStateException.class,s::markTicketIssued);
        s.markSeatReserved(); assertThrows(IllegalStateException.class,s::markSeatReserved);
        s.markTicketIssued(); assertThrows(IllegalStateException.class,s::markTicketIssued); s.markCompleted();
    }
}
