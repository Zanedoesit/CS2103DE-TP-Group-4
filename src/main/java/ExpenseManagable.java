import exceptions.ExpenseNotFoundException;

/**
 * Provides CRUD operations for expenses and total cost aggregation.
 */
public interface ExpenseManagable {
    void addExpense(Expense expense);

    void deleteExpenseById(int id) throws ExpenseNotFoundException;

    void deleteExpenseByName(String name) throws ExpenseNotFoundException;

    Expense getExpenseById(int id) throws ExpenseNotFoundException;

    Expense getExpenseByName(String name) throws ExpenseNotFoundException;

    float getTotalCost(Expense.Currency currency);
}
