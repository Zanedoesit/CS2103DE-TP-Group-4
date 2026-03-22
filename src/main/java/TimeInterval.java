import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Objects;

/**
 * Represents a time-bounded entity with overlap and duration helpers.
 */
public interface TimeInterval {

    LocalDateTime getStartDateTime();

    void setStartDateTime(LocalDateTime startDateTime);

    LocalDateTime getEndDateTime();

    void setEndDateTime(LocalDateTime endDateTime);

    default Boolean overlapsWith(TimeInterval other) {
        Objects.requireNonNull(other, "other");
        LocalDateTime start = Objects.requireNonNull(getStartDateTime(), "startDateTime");
        LocalDateTime end = Objects.requireNonNull(getEndDateTime(), "endDateTime");
        LocalDateTime otherStart = Objects.requireNonNull(other.getStartDateTime(), "other.startDateTime");
        LocalDateTime otherEnd = Objects.requireNonNull(other.getEndDateTime(), "other.endDateTime");
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    default Duration getDuration() {
        return Duration.between(
                Objects.requireNonNull(getStartDateTime(), "startDateTime"),
                Objects.requireNonNull(getEndDateTime(), "endDateTime"));
    }

    default Period getPeriod() {
        LocalDateTime start = Objects.requireNonNull(getStartDateTime(), "startDateTime");
        LocalDateTime end = Objects.requireNonNull(getEndDateTime(), "endDateTime");
        return Period.between(start.toLocalDate(), end.toLocalDate());
    }

}
