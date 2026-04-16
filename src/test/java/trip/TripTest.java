package trip;

import activity.Activity;
import exceptions.ActivityNotFoundException;
import exceptions.TimeIntervalConflictException;
import expense.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Trip}.
 * Verifies construction, time validation, activity CRUD, cost aggregation
 * across trip-level and activity-level expenses, and overlap detection.
 */
class TripTest {

    private static final LocalDateTime APR_3_0900 = LocalDateTime.of(2026, 4, 3, 9, 0);
    private static final LocalDateTime APR_5_1800 = LocalDateTime.of(2026, 4, 5, 18, 0);

    private Trip trip;

    @BeforeEach
    void setUp() {
        trip = new Trip(1, "Osaka Weekend", APR_3_0900, APR_5_1800);
    }


    /** Verifies that the constructor correctly assigns id, name, start, and end fields. */
    @Test
    void constructor_setsFieldsCorrectly() {
        assertEquals(1, trip.getId());
        assertEquals("Osaka Weekend", trip.getName());
        assertEquals(APR_3_0900, trip.getStartDateTime());
        assertEquals(APR_5_1800, trip.getEndDateTime());
    }

    /** Verifies that constructing a Trip with start after end throws {@link IllegalArgumentException}. */
    @Test
    void constructor_rejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trip(2, "Bad", APR_5_1800, APR_3_0900));
    }

    /** Verifies that a non-overlapping activity is added successfully. */
    @Test
    void addActivity_succeeds_whenNoOverlap() throws TimeIntervalConflictException {
        Activity a = new Activity(10, "Museum",
                LocalDateTime.of(2026, 4, 4, 10, 0),
                LocalDateTime.of(2026, 4, 4, 12, 0));
        trip.addActivity(a);
        assertEquals(1, trip.getActivities().size());
    }

    /** Verifies that overlapping activities are both stored, reflecting the design decision to allow overlaps. */
    @Test
    void addActivity_allowsOverlappingActivities() throws TimeIntervalConflictException {
        Activity a = new Activity(10, "Museum",
                LocalDateTime.of(2026, 4, 4, 10, 0),
                LocalDateTime.of(2026, 4, 4, 12, 0));
        trip.addActivity(a);

        Activity overlap = new Activity(11, "Brunch",
                LocalDateTime.of(2026, 4, 4, 11, 30),
                LocalDateTime.of(2026, 4, 4, 12, 30));
        trip.addActivity(overlap);
        assertEquals(2, trip.getActivities().size());
    }

    /** Verifies that {@code deleteActivityById} removes the matching activity. */
    @Test
    void deleteActivityById_removesActivity() throws Exception {
        Activity a = new Activity(10, "Museum",
                LocalDateTime.of(2026, 4, 4, 10, 0),
                LocalDateTime.of(2026, 4, 4, 12, 0));
        trip.addActivity(a);
        trip.deleteActivityById(10);
        assertTrue(trip.getActivities().isEmpty());
    }

    /** Verifies that {@code deleteActivityById} throws {@link ActivityNotFoundException} for a non-existent id. */
    @Test
    void deleteActivityById_throwsWhenNotFound() {
        assertThrows(ActivityNotFoundException.class,
                () -> trip.deleteActivityById(999));
    }

    /** Verifies that {@code getTotalCost} aggregates expenses from both the trip and its activities. */
    @Test
    void getTotalCost_aggregatesTripAndActivityExpenses() throws TimeIntervalConflictException {
        trip.addExpense(new Expense(1, "Visa fee", 50f, Expense.Currency.USD, Expense.Type.OTHER));

        Activity a = new Activity(10, "Museum",
                LocalDateTime.of(2026, 4, 4, 10, 0),
                LocalDateTime.of(2026, 4, 4, 12, 0));
        a.addExpense(new Expense(2, "Ticket", 25f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT));
        trip.addActivity(a);

        assertEquals(75f, trip.getTotalCost(Expense.Currency.USD), 0.01f);
    }

    /** Verifies that {@code getTotalCost} returns zero for a currency with no recorded expenses. */
    @Test
    void getTotalCost_returnsZeroForUnusedCurrency() {
        trip.addExpense(new Expense(1, "Visa", 50f, Expense.Currency.USD, Expense.Type.OTHER));
        assertEquals(0f, trip.getTotalCost(Expense.Currency.EUR));
    }

    /** Verifies that {@code getOverlappingActivities} returns an empty list when no activities overlap. */
    @Test
    void getOverlappingActivities_returnsEmptyWhenNoOverlaps() throws TimeIntervalConflictException {
        Activity a = new Activity(10, "Museum",
                LocalDateTime.of(2026, 4, 4, 10, 0),
                LocalDateTime.of(2026, 4, 4, 12, 0));
        trip.addActivity(a);
        assertTrue(trip.getOverlappingActivities().isEmpty());
    }
}