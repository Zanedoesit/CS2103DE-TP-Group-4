package filter;

import activity.Activity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ActivityFilter}.
 * Verifies that the stateless filtering utility correctly handles
 * null type (return all), matching, non-matching, and null input list.
 */
class ActivityFilterTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 4, 4, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 4, 4, 12, 0);

    private Activity createActivity(int id, String name, Activity.Type type) {
        Activity a = new Activity(id, name, START, END);
        a.addType(type);
        return a;
    }

    /** Verifies that passing null as the type returns all activities unfiltered. */
    @Test
    void byType_returnsAll_whenTypeIsNull() {
        List<Activity> activities = List.of(
                createActivity(1, "Museum", Activity.Type.CULTURAL),
                createActivity(2, "Hike", Activity.Type.ADVENTURE));
        List<Activity> result = ActivityFilter.byType(activities, null);
        assertEquals(2, result.size());
    }

    /** Verifies that only activities containing the specified type are returned. */
    @Test
    void byType_filtersCorrectly() {
        List<Activity> activities = List.of(
                createActivity(1, "Museum", Activity.Type.CULTURAL),
                createActivity(2, "Hike", Activity.Type.ADVENTURE),
                createActivity(3, "Temple", Activity.Type.CULTURAL));
        List<Activity> result = ActivityFilter.byType(activities, Activity.Type.CULTURAL);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(a -> a.getTypes().contains(Activity.Type.CULTURAL)));
    }

    /** Verifies that filtering by a type no activity has returns an empty list. */
    @Test
    void byType_returnsEmpty_whenNoMatch() {
        List<Activity> activities = List.of(
                createActivity(1, "Hike", Activity.Type.ADVENTURE));
        List<Activity> result = ActivityFilter.byType(activities, Activity.Type.RELAXATION);
        assertTrue(result.isEmpty());
    }

    /** Verifies that passing a null activity list throws {@link NullPointerException}. */
    @Test
    void byType_rejectsNullList() {
        assertThrows(NullPointerException.class,
                () -> ActivityFilter.byType(null, Activity.Type.CULTURAL));
    }
}