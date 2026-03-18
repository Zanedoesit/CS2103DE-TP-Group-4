import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.time.LocalDateTime;
import java.time.Period;

public class API {}

/* All fields will have its corresponsing getters and setters */
/* Optional logic not considered for now */

/* Exceptions */
class TripNotFoundException extends Exception {}


class ActivityNotFoundException extends Exception {}


class ExpenseNotFoundException extends Exception {}


class TimeIntervalConflictException extends Exception {}


/* Interfaces */


interface TimeInterval {

    LocalDateTime getStartDateTime();

    void setStartDateTime();

    LocalDateTime getEndDateTime();

    void setEndDateTime();

    Boolean overlapsWith(TimeInterval other);

    Duration getDuration();

    Period getPeriod();

}


interface ExpenseManagable {
    void addExpense(Expense expense);

    void deleteExpenseById(int id) throws ExpenseNotFoundException;

    void deleteExpenseByName(String name) throws ExpenseNotFoundException;

    Expense getExpenseById(int id) throws ExpenseNotFoundException;

    Expense getExpenseByName(String name) throws ExpenseNotFoundException;

    float getTotalCost(Expense.Currency currency);
}


interface Copyable<T> {

    T copy();

}



/* Abstract Classes */


abstract class BaseEntity {

    private int id;

    private String name;

    private String description;

    private BufferedImage image;

}


/* Concrete Classes */


class Application {

    private UI ui;

    private TripManager tripManager;

    private JsonStorage jsonStorage;

}


class UI {}


class Expense extends BaseEntity implements Copyable<Expense> {

    enum Type {
        FOOD,
        ACCOMMODATION,
        TRANSPORTATION,
        ENTERTAINMENT,
        OTHER,
        ...
    }

    enum Currency {
        USD,
        EUR,
        GBP,
        JPY,
        CNY,
        ...
    }

    private float cost;

}


class Location extends BaseEntity {

    private String address;

    private String city;

    private String country;

    private double latitude;

    private double longitude;

    public double distanceTo(Location other) {}

}



class Activity extends BaseEntity implements TimeInterval, ExpenseManagable, Copyable<Activity> {

    enum Type {
        SIGHTSEEING,
        ADVENTURE,
        RELAXATION,
        CULTURAL,
        OTHER,
        ...
    }

    private List<Type> types;

    private List<Expense> expenses;

    private Location location;

}

class Trip extends BaseEntity implements TimeInterval, ExpenseManagable, Copyable<Trip> {

    private List<Activity> activities;

    private List<Expense> expenses;

    private Location location;

    public void addActivity(Activity activity) throws TimeIntervalConflictException {}

    public void deleteActivityById(int id) throws ActivityNotFoundException {}

    public void deleteActivityByName(String name) throws ActivityNotFoundException {}

    public List<Activity> getOverlappingActivities() {}

    public List<Activity> getOverlappingAcitivites(LocalDateTime begin, LocalDateTime end) {}

}


class TripManager {

    List<Trip> trips;

    public void addTrip(Trip trip) throws TimeIntervalConflictException{}

    public void deleteTripById(int id) throws TripNotFoundException {}

    public void deleteTripByName(String name) throws TripNotFoundException {}

    public Trip getTripById(int id) throws TripNotFoundException {}

    public Trip getTripByName(String name) throws TripNotFoundException {}

    public List<Trip> getOverlappingTrips() {}

    public List<Trip> getOverlappingTrips(LocalDateTime begin, LocalDateTime end) {}

}


class JsonStorage {

    private String filePath;

    public void saveTrips(List<Trip> trips) {}

    public List<Trip> loadTrips() {}

}

/* Placeholder for javax.json package */

class Json {}


class JsonObject {}

