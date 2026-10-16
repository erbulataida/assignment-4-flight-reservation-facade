package kz.aida.flight;
import java.nio.file.Path;
import java.util.Scanner;
import kz.aida.flight.orchestration.*;
public class Main {
    public static void main(String[] args) {
        Path directory=Path.of(args.length==0 ? "data" : args[0]);
        var booking=FlightBookingFacade.open(directory);
        var admin=new BookingAdminFacade(directory,booking);
        booking.recoverPendingSagas();
        Scanner in=new Scanner(System.in);
        while (true) {
            System.out.println("1 Book | 2 Inspect | 3 Manual compensation | 4 Pause after A | 5 Pause after B | 0 Exit");
            if (!in.hasNextLine()) return;
            String command=in.nextLine();
            try {
                if (command.equals("0")) return;
                if (command.equals("2")) { System.out.print(admin.inspect()); continue; }
                if (command.equals("3")) { System.out.println("Saga ID:"); admin.compensate(in.nextLine()); continue; }
                if (!java.util.List.of("1","4","5").contains(command)) { System.out.println("Unknown option"); continue; }
                System.out.println("Flight (demo capacity 3):"); String flight=in.nextLine();
                System.out.println("Passenger:"); String passenger=in.nextLine();
                System.out.println("Baggage kg (35 fails at B):"); double kg=Double.parseDouble(in.nextLine());
                System.out.println("Meal STANDARD / VEGETARIAN / HALAL (KOSHER fails at C):"); String meal=in.nextLine();
                String id=booking.startBooking(flight,passenger,kg,meal);
                var result=booking.continueBooking(id,command.equals("4") ? 1 : command.equals("5") ? 2 : 3);
                System.out.println("Saga " + id + ": " + (result.isCompleted() ? "COMPLETED" : result.getErrorMessage()));
                System.out.print(admin.inspect());
            } catch (IllegalArgumentException | java.util.NoSuchElementException e) {
                System.out.println("Request rejected: " + e.getMessage());
            }
        }
    }
}
