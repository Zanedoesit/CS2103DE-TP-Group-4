package exceptions;

/**
 * Thrown when an expense lookup fails.
 */
public class ExpenseNotFoundException extends Exception {
    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
