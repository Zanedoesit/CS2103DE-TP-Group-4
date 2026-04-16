package trip;

import exceptions.TimeIntervalConflictException;
import exceptions.TripNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TripManager}.
 * Verifies trip addition, overlap rejection, deletion, lookup, and overlap reporting.
 * Each test uses unique ids and names to avoid collisions with the static uniqueness sets.
 */
class TripManagerTest {

    private TripManager manager;

    @BeforeEach
    void setUp() {
        manager = new TripManager();
    }

    /** Verifies that a valid trip is added and appears in the trips list. */
    @Test
    void addTrip_addsSuccessfully() throws TimeIntervalConflictException {
        Trip trip = new Trip(101, "Tokyo101",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 5, 18, 0));
        manager.addTrip(trip);
        assertEquals(1, manager.getTrips().size());
    }

    /** Verifies that adding a trip whose dates overlap an existing trip throws {@link TimeIntervalConflictException}. */
    @Test
    void addTrip_throwsOnOverlap() throws TimeIntervalConflictException {
        Trip trip1 = new Trip(201, "Tokyo201",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 5, 18, 0));
        manager.addTrip(trip1);

        Trip trip2 = new Trip(202, "Osaka202",
                LocalDateTime.of(2026, 5, 3, 9, 0),
                LocalDateTime.of(2026, 5, 7, 18, 0));
        assertThrows(TimeIntervalConflictException.class,
                () -> manager.addTrip(trip2));
    }

    /** Verifies that {@code deleteTripById} removes the trip from the manager. */
    @Test
    void deleteTripById_removesTrip() throws Exception {
        Trip trip = new Trip(301, "Tokyo301",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 5, 18, 0));
        manager.addTrip(trip);
        manager.deleteTripById(301);
        assertTrue(manager.getTrips().isEmpty());
    }

    /** Verifies that {@code deleteTripById} throws {@link TripNotFoundException} for a non-existent id. */
    @Test
    void deleteTripById_throwsWhenNotFound() {
        assertThrows(TripNotFoundException.class,
                () -> manager.deleteTripById(999));
    }

    /** Verifies that {@code getTripById} returns the correct trip when it exists. */
    @Test
    void getTripById_findsTrip() throws Exception {
        Trip trip = new Trip(501, "Tokyo501",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 5, 18, 0));
        manager.addTrip(trip);
        Trip found = manager.getTripById(501);
        assertEquals("Tokyo501", found.getName());
    }

    /** Verifies that {@code getOverlappingTrips} returns an empty list when no trips overlap. */
    @Test
    void getOverlappingTrips_returnsEmptyWhenNone() throws TimeIntervalConflictException {
        Trip trip = new Trip(601, "Tokyo601",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 5, 18, 0));
        manager.addTrip(trip);
        assertTrue(manager.getOverlappingTrips().isEmpty());
    }
}