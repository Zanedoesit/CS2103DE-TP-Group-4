package filter;

import activity.Activity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides reusable, stateless filtering operations on Activity lists.
 *
 * <p>Design rationale: filtering logic is separated from the UI controller
 * (Single Responsibility Principle) so it can be unit-tested independently
 * and reused by any future UI or CLI layer.</p>
 */
public final class ActivityFilter {

    /** Prevents instantiation — all methods are static utility methods. */
    private ActivityFilter() {
    }

    /**
     * Returns only the activities whose {@code types} list contains the given type.
     *
     * @param activities the full list of activities (must not be null)
     * @param type       the type to match; if null, all activities are returned
     * @return an unmodifiable filtered list
     */
    public static List<Activity> byType(List<Activity> activities, Activity.Type type) {
        Objects.requireNonNull(activities, "activities");
        if (type == null) {
            return Collections.unmodifiableList(activities);
        }
        return activities.stream()
                .filter(a -> a.getTypes().contains(type))
                .collect(Collectors.toUnmodifiableList());
    }
}