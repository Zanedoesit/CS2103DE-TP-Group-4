package expense;
import java.util.Locale;
import java.util.Objects;

import utilities.BaseEntity;
import utilities.Copyable;

/**
 * A single cost line item.
 */
public class Expense extends BaseEntity implements Copyable<Expense> {

    public enum Type {
        FOOD,
        ACCOMMODATION,
        TRANSPORTATION,
        ENTERTAINMENT,
        OTHER
    }

    public enum Currency {
        SGD,
        USD,
        EUR,
        GBP,
        JPY,
        CNY
    }

    private float cost;

    private Currency currency;

    private Type type;

    private String imagePath;

    public Expense(int id, String name, float cost, Currency currency, Type type) {
        this(id, name, cost, currency, type, null);
    }

    public Expense(int id, String name, float cost, Currency currency, Type type, String imagePath) {
        super(id, name);
        setCost(cost);
        setCurrency(currency);
        setType(type);
        setImagePath(imagePath);
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost must be non-negative");
        }
        this.cost = cost;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        if (imagePath == null) {
            this.imagePath = null;
            return;
        }
        String trimmed = imagePath.trim();
        this.imagePath = trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public Expense copy() {
        Expense copy = new Expense(getId(), getName(), cost, currency, type, imagePath);
        copy.setDescription(getDescription());
        copy.setPriority(getPriority());
        copy.setImage(getImage());
        return copy;
    }

    @Override
    public String toString() {
        return "Expense #" + getId() + ": " + getName()
                + " | " + String.format(Locale.US, "%.2f", getCost()) + " " + getCurrency()
                + " | Type: " + getType();
    }
}
