package exceptions;

/**
 * Thrown when a new interval overlaps an existing interval.
 */
public class TimeIntervalConflictException extends Exception {
    public TimeIntervalConflictException(String message) {
        super(message);
    }
}
