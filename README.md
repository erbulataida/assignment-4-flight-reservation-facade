# Flight Reservation — Assignment 4

Java 17 / Maven / durable pipe-delimited CSV. Individual domain: variant 8.

## Run

Install JDK 17+ and Maven 3.9+. From this directory:

```sh
mvn compile exec:java
```

Tests and HTML coverage: `mvn test`. Enforce 80% line coverage for **each** of the three subsystem packages and the orchestration package: `mvn verify`. Open `target/site/jacoco/index.html`. The console entry point is excluded; business and facade code is included. No external service or database is needed.

Only run one application process per data directory. Files use UTF-8 and `|` separators; pipes and newlines in text are rejected. Individual file replacements are atomic where supported by the filesystem, with replace fallback. This is a single-process teaching application, not a concurrent booking platform or a power-loss-safe database.

## Live demonstration

| Goal | Menu and input | Persisted outcome |
| --- | --- | --- |
| Successful A → B → C | 1; KC101; Aida; 20; HALAL | RESERVED / ISSUED / CONFIRMED; saga COMPLETED |
| B fails after A | 1; KC102; Dana; 35; STANDARD | A CANCELLED; no B/C; saga COMPENSATED |
| C fails after A and B | 1; KC103; Aliya; 20; KOSHER | B VOIDED, then A CANCELLED; no C; saga COMPENSATED |
| Restart after A | 4; KC104; Aida; 20; STANDARD; then 0 | SEAT_RESERVED; run application again → COMPLETED |
| Restart after B, C rejects | 5; KC105; Dana; 20; KOSHER; then 0 | TICKET_ISSUED; restart → B VOIDED, A CANCELLED |
| Inspect | 2 | Read sagas, seats/fares, tickets, meals/points from disk |
| Manual cancellation | 3; existing saga ID | Revoke C if present, void B, cancel A; repeat is safe |
| Extra realistic A failure | Book 3 passengers on SAME flight, then a fourth | Fourth request rejected: flight full, no ticket for it |
| Invalid input | Negative / NaN baggage, empty name, malformed number | Request rejected; menu remains usable |

Use different flight numbers to avoid unintentionally filling the three-seat demonstration aircraft. Available meals: STANDARD, VEGETARIAN, HALAL. Unsupported meals represent unavailable catering, not a mocked exception. Loyalty awards 100 points on confirmation; cancellation revokes them.

Checkpoints 4/5 return a paused message. Exit before rerunning to demonstrate a fresh process. Startup automatically recovers all unfinished sagas. The app does not keep a store connection open: each repository operation reads/writes a file, so constructing a new facade reopens all persisted state.

Reset only while the application is stopped: delete the generated `data` folder (`rm -rf data` on macOS/Linux). This removes demo bookings. A fresh run recreates it. To use a separate store: `mvn compile exec:java -Dexec.args=demo-data`.

## Design

| Subsystem | Classes (excluding enums) | Business rule | Own persistence gateway |
| --- | --- | --- | --- |
| A Seating | SeatReservation, SeatReservationService, SeatReservationRepository, SeatMap, FareCalculator | Three seats per flight; front-row fare 80000, other seats 40000 KZT | SeatReservationRepository |
| B Ticketing | ETicket, TicketingService, ETicketRepository, BaggageRulesEngine, TicketIssuanceException | Finite non-negative baggage, max 30 kg | ETicketRepository |
| C Preferences | TravelPreference, LoyaltyMealService, PreferenceRepository, MealRulesEngine | Supported meal and loyalty award | PreferenceRepository |

Entities own their status transitions: NEW → RESERVED → CANCELLED; NEW → ISSUED → VOIDED; NEW → CONFIRMED → CANCELLED. Services persist these entity decisions. No subsystem imports another subsystem. Shared CsvStore is infrastructure only; Saga belongs to orchestration, not to a fourth domain subsystem.

FlightBookingFacade coordinates A → B → C. BookingAdminFacade provides disk inspection and manual cancellation. Main talks to these facades only. The static `open` factory is the composition root; service dependencies can also be injected for focused tests.

### Durable recovery and compensation

The request (including meal), preallocated reservation/ticket IDs, and progress are saved in sagas.csv. Progress: STARTED → SEAT_RESERVED → TICKET_ISSUED → COMPLETED. Cancellation: COMPENSATING → COMPENSATED. Before a resumed step, the facade checks actual entity records/statuses. It handles the crash window where an entity was saved but the saga progress was not. A preference uses the saga ID as its independent correlation key; its service does not know what a saga or ticket is.

For a rejected B, there is no ticket: compensate A only. For a rejected C, void the persisted ticket first, then cancel the seat. Persist COMPENSATING before undoing anything. On restart, inspect statuses again and undo only the remaining active records. This makes retry safe after a partial compensation. Manual cancellation of a completed booking also revokes confirmed preferences first. Storage failures propagate instead of being disguised as domain rejection; a later startup retries pending work after the store is repaired.

CSV is not a cross-file transaction. Correctness comes from durable intent, preallocated IDs, checking records before replay, and idempotent orchestration. Direct service operations enforce transitions and reject duplicate IDs. Direct operations use their own IDs; tests demonstrate that each subsystem works without either of the other two.

### Principle of Least Knowledge: concrete before / after

Before (client must understand the three services, order, progress and reversals):

```java
SeatReservation reservation = seats.reserveSeat("R1", "KC101", "Aida");
ETicket ticket = tickets.issueTicket("T1", reservation.getId(), "Aida", 20);
// If this fails, the client must void ticket and then cancel reservation.
preferences.confirm("P1", "Aida", "HALAL");
```

After:

```java
FlightBookingFacade booking = FlightBookingFacade.open(Path.of("data"));
FlightBookingResult result = booking.bookFlight("KC101", "Aida", 20, "HALAL");
```

The client knows one workflow facade and its result. It no longer traverses services, repositories and entities to coordinate a booking. Administration similarly uses BookingAdminFacade.

### Facade vs Adapter (reflection)

Our client wants one booking, but seating, ticketing and travel preferences are separate services with different responsibilities. Their interfaces already work: SeatReservationService reserves seats, TicketingService issues tickets and LoyaltyMealService confirms meals and loyalty points. There is no incompatible interface that needs conversion.

The difficulty is coordinating their use: choose a seat, issue a ticket, then finalize preferences; persist progress; detect rejection; and reverse the correct committed steps. FlightBookingFacade supplies a coherent higher-level operation and hides this coordination from the client. BookingAdminFacade supplies a smaller administrative view over the same services. Both reduce how much clients must know, while the underlying services remain available independently.

An Adapter would make an existing service fit an expected interface, for example translating a legacy airline ticket API to a new ticket interface. That could be useful inside ticketing, but would not by itself define the three-stage booking workflow or its recovery policy. The choice here follows the intent of simplifying coordinated access, rather than the number of classes.

## Tests and F.I.R.S.T.

`BookingTest` includes both rollback depths, persisted end states, explicit B-before-A call order, restart after A/B, entity-save-before-progress windows, restart during compensation, administrative cancellation, input validation and direct subsystem use. `aPassengerBooksWith` is a small domain-specific testing language. The direct-subsystem test runs all three services independently using unrelated IDs.

Each JUnit test receives its own @TempDir. No test uses production data, network access, sleeps or another test's state. Assertions are self-validating. Files are tiny, keeping tests fast and repeatable. New tests are committed alongside the completed implementation. The uploaded ZIP contained no Git history; the supplied local history records the imported baseline and the completion work honestly, and cannot establish when earlier code was originally tested.

See [UML class diagram](docs/uml.svg), [editable diagram source](docs/uml.mmd) and [Russian study guide](docs/DEFENSE_RU.md).

## Validation snapshot

`mvn verify`: 17 tests, 0 failures/errors. JaCoCo line coverage: seating 100/101 (99.01%), ticketing 91/92 (98.91%), preferences 43/43 (100%), orchestration 151/154 (98.05%). The coverage gate passed. Console smoke checks exercised success, B rejection, C rejection, and a real new-process restart after A.

## Submission

Push this local repository to your own GitHub repository and submit its URL to Moodle before the instructor's deadline. Run `mvn verify` and rehearse the scenarios above first. No remote repository or Moodle submission is performed by this archive.
