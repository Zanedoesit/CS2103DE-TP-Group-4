import exceptions.TimeIntervalConflictException;
import exceptions.TripNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Registry for all trips in the application.
 */
public class TripManager {

    private final List<Trip> trips = new ArrayList<>();

    public TripManager() {
        this(Collections.emptyList());
    }

    public TripManager(List<Trip> trips) {
        if (trips != null) {
            this.trips.addAll(trips);
        }
    }

    public List<Trip> getTrips() {
        return Collections.unmodifiableList(trips);
    }

    public void addTrip(Trip trip) throws TimeIntervalConflictException {
        Objects.requireNonNull(trip, "trip");
        for (Trip existing : trips) {
            if (trip.overlapsWith(existing)) {
                throw new TimeIntervalConflictException(
                        "Trip time conflict with existing trip: " + existing.getName());
            }
        }
        trips.add(trip);
    }

    public void deleteTripById(int id) throws TripNotFoundException {
        Trip trip = findTripById(id);
        trips.remove(trip);
    }

    public void deleteTripByName(String name) throws TripNotFoundException {
        Trip trip = findTripByName(name);
        trips.remove(trip);
    }

    public Trip getTripById(int id) throws TripNotFoundException {
        return findTripById(id);
    }

    public Trip getTripByName(String name) throws TripNotFoundException {
        return findTripByName(name);
    }

    public List<Trip> getOverlappingTrips() {
        List<Trip> result = new ArrayList<>();
        for (int i = 0; i < trips.size(); i++) {
            Trip current = trips.get(i);
            for (int j = i + 1; j < trips.size(); j++) {
                Trip other = trips.get(j);
                if (current.overlapsWith(other)) {
                    if (!result.contains(current)) {
                        result.add(current);
                    }
                    if (!result.contains(other)) {
                        result.add(other);
                    }
                }
            }
        }
        return result;
    }

    public List<Trip> getOverlappingTrips(LocalDateTime begin, LocalDateTime end) {
        Objects.requireNonNull(begin, "begin");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(begin)) {
            throw new IllegalArgumentException("end must not be before begin");
        }
        List<Trip> result = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.getStartDateTime().isBefore(end) && trip.getEndDateTime().isAfter(begin)) {
                result.add(trip);
            }
        }
        return result;
    }

    private Trip findTripById(int id) throws TripNotFoundException {
        for (Trip trip : trips) {
            if (trip.getId() == id) {
                return trip;
            }
        }
        throw new TripNotFoundException("Trip not found: id=" + id);
    }

    private Trip findTripByName(String name) throws TripNotFoundException {
        for (Trip trip : trips) {
            if (Objects.equals(trip.getName(), name)) {
                return trip;
            }
        }
        throw new TripNotFoundException("Trip not found: name=" + name);
    }

}
