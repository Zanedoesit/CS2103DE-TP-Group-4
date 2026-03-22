package exceptions;

/**
 * Thrown when a trip lookup fails.
 */
public class TripNotFoundException extends Exception {
    public TripNotFoundException(String message) {
        super(message);
    }
}
