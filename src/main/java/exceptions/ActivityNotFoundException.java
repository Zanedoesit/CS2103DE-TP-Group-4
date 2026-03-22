package exceptions;

/**
 * Thrown when an activity lookup fails.
 */
public class ActivityNotFoundException extends Exception {
    public ActivityNotFoundException(String message) {
        super(message);
    }
}
