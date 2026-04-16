package activity;

import expense.Expense;
import exceptions.ExpenseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Activity}.
 * Verifies construction, time validation, type tagging, expense CRUD,
 * overlap detection, and deep copy behavior.
 */
class ActivityTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 4, 4, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 4, 4, 12, 0);

    private Activity activity;

    @BeforeEach
    void setUp() {
        activity = new Activity(10, "Museum", START, END);
    }

    /** Verifies that the constructor correctly assigns id, name, start, and end fields. */
    @Test
    void constructor_setsFieldsCorrectly() {
        assertEquals(10, activity.getId());
        assertEquals("Museum", activity.getName());
        assertEquals(START, activity.getStartDateTime());
        assertEquals(END, activity.getEndDateTime());
    }

    /** Verifies that constructing an Activity with start after end throws {@link IllegalArgumentException}. */
    @Test
    void constructor_rejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> new Activity(1, "Bad", END, START));
    }

    /** Verifies that {@code addType} appends a type to the activity's type list. */
    @Test
    void addType_addsTypeToList() {
        activity.addType(Activity.Type.CULTURAL);
        assertEquals(1, activity.getTypes().size());
        assertEquals(Activity.Type.CULTURAL, activity.getTypes().get(0));
    }

    /** Verifies that {@code getTotalCost} sums all expenses matching the given currency. */
    @Test
    void addExpense_andGetTotalCost() {
        activity.addExpense(new Expense(1, "Ticket", 25f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT));
        activity.addExpense(new Expense(2, "Guide", 10f, Expense.Currency.USD, Expense.Type.OTHER));
        assertEquals(35f, activity.getTotalCost(Expense.Currency.USD), 0.01f);
    }

    /** Verifies that {@code deleteExpenseById} removes the matching expense from the list. */
    @Test
    void deleteExpenseById_removesExpense() throws ExpenseNotFoundException {
        activity.addExpense(new Expense(1, "Ticket", 25f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT));
        activity.deleteExpenseById(1);
        assertTrue(activity.getExpenses().isEmpty());
    }

    /** Verifies that {@code deleteExpenseById} throws {@link ExpenseNotFoundException} for a non-existent id. */
    @Test
    void deleteExpenseById_throwsWhenNotFound() {
        assertThrows(ExpenseNotFoundException.class,
                () -> activity.deleteExpenseById(999));
    }

    /** Verifies that {@code overlapsWith} returns true when two activities share a time window. */
    @Test
    void overlapsWith_detectsOverlap() {
        Activity other = new Activity(11, "Brunch",
                LocalDateTime.of(2026, 4, 4, 11, 30),
                LocalDateTime.of(2026, 4, 4, 12, 30));
        assertTrue(activity.overlapsWith(other));
    }

    /** Verifies that {@code overlapsWith} returns false when two activities have no shared time. */
    @Test
    void overlapsWith_returnsFalseForNonOverlap() {
        Activity other = new Activity(11, "Dinner",
                LocalDateTime.of(2026, 4, 4, 18, 0),
                LocalDateTime.of(2026, 4, 4, 20, 0));
        assertFalse(activity.overlapsWith(other));
    }

    /** Verifies that {@code copy} produces a deep copy with independent expense objects. */
    @Test
    void copy_createsDeepCopy() {
        activity.addType(Activity.Type.SIGHTSEEING);
        activity.addExpense(new Expense(1, "Ticket", 25f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT));
        Activity copy = activity.copy();
        assertEquals(activity.getName(), copy.getName());
        assertEquals(1, copy.getExpenses().size());
        assertNotSame(activity.getExpenses().get(0), copy.getExpenses().get(0));
    }
}