package expense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Expense}.
 * Verifies construction, field validation, null safety, and deep copy independence.
 */
class ExpenseTest {

    /** Verifies that the constructor correctly assigns id, name, cost, currency, and type. */
    @Test
    void constructor_setsFieldsCorrectly() {
        Expense e = new Expense(1, "Ticket", 25f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT);
        assertEquals(1, e.getId());
        assertEquals("Ticket", e.getName());
        assertEquals(25f, e.getCost());
        assertEquals(Expense.Currency.USD, e.getCurrency());
        assertEquals(Expense.Type.ENTERTAINMENT, e.getType());
    }

    /** Verifies that {@code setCost} throws {@link IllegalArgumentException} for negative values. */
    @Test
    void setCost_rejectsNegative() {
        Expense e = new Expense(1, "Ticket", 10f, Expense.Currency.USD, Expense.Type.OTHER);
        assertThrows(IllegalArgumentException.class, () -> e.setCost(-5f));
    }

    /** Verifies that {@code copy} creates an independent instance whose mutations do not affect the original. */
    @Test
    void copy_createsIndependentCopy() {
        Expense original = new Expense(1, "Ticket", 25f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT);
        original.setDescription("museum entry");
        Expense copy = original.copy();

        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getCost(), copy.getCost());
        assertEquals(original.getDescription(), copy.getDescription());

        copy.setCost(50f);
        assertEquals(25f, original.getCost());
    }

    /** Verifies that {@code setCurrency} throws {@link NullPointerException} when given null. */
    @Test
    void setCurrency_rejectsNull() {
        Expense e = new Expense(1, "Ticket", 10f, Expense.Currency.USD, Expense.Type.OTHER);
        assertThrows(NullPointerException.class, () -> e.setCurrency(null));
    }

    /** Verifies that {@code setType} throws {@link NullPointerException} when given null. */
    @Test
    void setType_rejectsNull() {
        Expense e = new Expense(1, "Ticket", 10f, Expense.Currency.USD, Expense.Type.OTHER);
        assertThrows(NullPointerException.class, () -> e.setType(null));
    }
}