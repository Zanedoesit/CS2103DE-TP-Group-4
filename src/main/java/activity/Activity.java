package activity;
import exceptions.ExpenseNotFoundException;
import expense.Expense;
import expense.ExpenseManagable;
import expense.Expense.Currency;
import location.Location;
import temporal.TimeInterval;
import utilities.BaseEntity;
import utilities.Copyable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A time-bounded activity within a trip.
 */
public class Activity extends BaseEntity implements TimeInterval, ExpenseManagable, Copyable<Activity> {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public enum Type {
        SIGHTSEEING,
        ADVENTURE,
        RELAXATION,
        CULTURAL,
        OTHER
    }

    private final List<Type> types = new ArrayList<>();

    private final List<Expense> expenses = new ArrayList<>();

    private Location location;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    public Activity(int id, String name, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this(id, name, startDateTime, endDateTime, null);
    }

    public Activity(int id, String name, LocalDateTime startDateTime, LocalDateTime endDateTime, Location location) {
        super(id, name);
        setStartDateTime(startDateTime);
        setEndDateTime(endDateTime);
        this.location = location;
    }

    public List<Type> getTypes() {
        return Collections.unmodifiableList(types);
    }

    public void setTypes(List<Type> types) {
        this.types.clear();
        if (types != null) {
            this.types.addAll(types);
        }
    }

    public void addType(Type type) {
        this.types.add(Objects.requireNonNull(type, "type"));
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses.clear();
        if (expenses != null) {
            for (Expense expense : expenses) {
                this.expenses.add(Objects.requireNonNull(expense, "expense"));
            }
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    @Override
    public void setStartDateTime(LocalDateTime startDateTime) {
        Objects.requireNonNull(startDateTime, "startDateTime");
        if (endDateTime != null && startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("startDateTime must not be after endDateTime");
        }
        this.startDateTime = startDateTime;
    }

    @Override
    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    @Override
    public void setEndDateTime(LocalDateTime endDateTime) {
        Objects.requireNonNull(endDateTime, "endDateTime");
        if (startDateTime != null && endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("endDateTime must not be before startDateTime");
        }
        this.endDateTime = endDateTime;
    }

    @Override
    public void addExpense(Expense expense) {
        expenses.add(Objects.requireNonNull(expense, "expense"));
    }

    @Override
    public void deleteExpenseById(int id) throws ExpenseNotFoundException {
        Expense expense = findExpenseById(id);
        expenses.remove(expense);
    }

    @Override
    public void deleteExpenseByName(String name) throws ExpenseNotFoundException {
        Expense expense = findExpenseByName(name);
        expenses.remove(expense);
    }

    @Override
    public Expense getExpenseById(int id) throws ExpenseNotFoundException {
        return findExpenseById(id);
    }

    @Override
    public Expense getExpenseByName(String name) throws ExpenseNotFoundException {
        return findExpenseByName(name);
    }

    @Override
    public float getTotalCost(Expense.Currency currency) {
        Objects.requireNonNull(currency, "currency");
        float total = 0f;
        for (Expense expense : expenses) {
            if (expense.getCurrency() == currency) {
                total += expense.getCost();
            }
        }
        return total;
    }

    @Override
    public Activity copy() {
        Activity copy = new Activity(getId(), getName(), startDateTime, endDateTime, location);
        copy.setDescription(getDescription());
        copy.setPriority(getPriority());
        copy.setImage(getImage());
        copy.setTypes(types);
        for (Expense expense : expenses) {
            copy.addExpense(expense.copy());
        }
        return copy;
    }

    private Expense findExpenseById(int id) throws ExpenseNotFoundException {
        for (Expense expense : expenses) {
            if (expense.getId() == id) {
                return expense;
            }
        }
        throw new ExpenseNotFoundException("Expense not found: id=" + id);
    }

    private Expense findExpenseByName(String name) throws ExpenseNotFoundException {
        for (Expense expense : expenses) {
            if (Objects.equals(expense.getName(), name)) {
                return expense;
            }
        }
        throw new ExpenseNotFoundException("Expense not found: name=" + name);
    }

    @Override
    public String toString() {
        StringJoiner typeJoiner = new StringJoiner(", ");
        for (Type type : types) {
            typeJoiner.add(type.name());
        }
        String typesText = typeJoiner.length() == 0 ? "None" : typeJoiner.toString();
        return "Activity #" + getId() + ": " + getName()
                + " | " + formatDateTime(getStartDateTime()) + " -> " + formatDateTime(getEndDateTime())
                + " | Location: " + (getLocation() != null ? getLocation() : "No location")
                + " | Types: " + typesText;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : "?";
    }
}
