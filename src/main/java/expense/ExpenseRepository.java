package expense;

import storage.ExpenseStorage;
import storage.ImageAssetStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Storage-backed repository for expense records.
 */
public class ExpenseRepository {

    private static final Set<Integer> USED_EXPENSE_IDS = new HashSet<>();

    private final List<Expense> expenses = new ArrayList<>();
    private final Map<Integer, Expense> expensesById = new HashMap<>();
    private final ExpenseStorage storage;
    private final ImageAssetStore imageAssetStore;
    private int nextId = 1;

    public ExpenseRepository() {
        this(new ExpenseStorage(), new ImageAssetStore());
    }

    public ExpenseRepository(ExpenseStorage storage, ImageAssetStore imageAssetStore) {
        this.storage = storage;
        this.imageAssetStore = imageAssetStore;
    }

    public void load() throws IOException {
        expenses.clear();
        expensesById.clear();
        USED_EXPENSE_IDS.clear();
        expenses.addAll(storage.load());
        nextId = 1;

        boolean hasUpdates = false;
        for (Expense expense : expenses) {
            registerExpenseId(expense.getId());
            nextId = Math.max(nextId, expense.getId() + 1);
            String normalizedPath = imageAssetStore.normalizeImagePath(expense.getImagePath());
            if (normalizedPath != null && !normalizedPath.equals(expense.getImagePath())) {
                expense.setImagePath(normalizedPath);
                hasUpdates = true;
            }
            expensesById.put(expense.getId(), expense);
        }

        if (hasUpdates) {
            save();
        }
    }

    public void save() throws IOException {
        storage.save(expenses);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public Expense findById(int id) {
        return expensesById.get(id);
    }

    public int nextAvailableId() {
        while (USED_EXPENSE_IDS.contains(nextId)) {
            nextId++;
        }
        return nextId;
    }

    public Expense createExpense(String name, float cost, Expense.Currency currency, Expense.Type type,
                                 String imageSourcePath) {
        int id = nextAvailableId();
        String normalizedName = normalizeRequired(name, "expense name");
        String imagePath = imageAssetStore.importImage(imageSourcePath, "expense", normalizedName);
        Expense expense = new Expense(id, normalizedName, cost, currency, type, imagePath);
        registerExpense(expense);
        return expense;
    }

    public Expense updateExpense(int expenseId, String name, float cost, Expense.Currency currency, Expense.Type type,
                                 String imageSourcePath) {
        Expense expense = expensesById.get(expenseId);
        if (expense == null) {
            throw new IllegalArgumentException("Expense not found: id=" + expenseId);
        }

        String normalizedName = normalizeRequired(name, "expense name");
        expense.setName(normalizedName);
        expense.setCost(cost);
        expense.setCurrency(currency);
        expense.setType(type);

        if (imageSourcePath != null && !imageSourcePath.isBlank()) {
            String storedImagePath = imageAssetStore.importImage(imageSourcePath, "expense", normalizedName);
            if (storedImagePath == null) {
                storedImagePath = imageAssetStore.normalizeImagePath(imageSourcePath);
            }
            expense.setImagePath(storedImagePath);
        }
        return expense;
    }

    public void registerExpense(Expense expense) {
        registerExpenseId(expense.getId());
        expenses.add(expense);
        expensesById.put(expense.getId(), expense);
        nextId = Math.max(nextId, expense.getId() + 1);
    }

    private void registerExpenseId(int id) {
        if (!USED_EXPENSE_IDS.add(id)) {
            throw new IllegalStateException("Duplicate expense id detected: " + id);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
