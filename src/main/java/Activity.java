import exceptions.ExpenseNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A time-bounded activity within a trip.
 */
public class Activity extends BaseEntity implements TimeInterval, ExpenseManagable, Copyable<Activity> {

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

}
