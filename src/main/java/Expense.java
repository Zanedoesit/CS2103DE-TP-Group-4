import java.util.Objects;

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
        USD,
        EUR,
        GBP,
        JPY,
        CNY
    }

    private float cost;

    private Currency currency;

    private Type type;

    public Expense(int id, String name, float cost, Currency currency, Type type) {
        super(id, name);
        setCost(cost);
        setCurrency(currency);
        setType(type);
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

    @Override
    public Expense copy() {
        Expense copy = new Expense(getId(), getName(), cost, currency, type);
        copy.setDescription(getDescription());
        copy.setImage(getImage());
        return copy;
    }

}
