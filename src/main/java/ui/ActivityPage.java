package ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import activity.Activity;
import expense.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import expense.Expense.Currency;
import expense.Expense.Type;
import javafx.scene.control.ListCell;

public class ActivityPage {
    @FXML
    private Label activityNameLabel;
    @FXML
    private Label activityDateLabel;
    @FXML
    private Label activityLocationLabel;
    @FXML
    private ListView<Expense> expenseListView;
    @FXML
    private Button addExpenseButton;
    @FXML
    private Button backButton;

    private Activity activity;
    private ObservableList<Expense> expenseObservableList = FXCollections.observableArrayList();
    private TripPage tripPage;
    private trip.TripManager tripManager;

    public void setActivity(Activity activity) {
        this.activity = activity;
        activityNameLabel.setText(activity.getName());
        activityDateLabel.setText(activity.getStartDateTime() + " - " + activity.getEndDateTime());
        activityLocationLabel.setText(activity.getLocation() != null ? activity.getLocation().toString() : "");
        expenseObservableList.setAll(activity.getExpenses());
    }

    public void setTripPage(TripPage tripPage) {
        this.tripPage = tripPage;
    }

    public void setTripManager(trip.TripManager tripManager) {
        this.tripManager = tripManager;
    }

    @FXML
    private void initialize() {
        expenseListView.setItems(expenseObservableList);
        // Set custom cell factory for expense list
        expenseListView.setCellFactory(list -> new ListCell<Expense>() {
            @Override
            protected void updateItem(Expense expense, boolean empty) {
                super.updateItem(expense, empty);
                if (empty || expense == null) {
                    setText(null);
                } else {
                    setText(expense.getName() + " - " + expense.getCost() + " " + expense.getCurrency() + " (" + expense.getType() + ")");
                }
            }
        });
        backButton.setOnAction(e -> {
            if (tripPage != null) {
                tripPage.showTripPage();
            }
        });
        addExpenseButton.setOnAction(e -> handleAddExpense());
    }

    private void handleAddExpense() {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle("Add Expense");
        dialog.setHeaderText("Enter expense details");
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField costField = new TextField("100.0");
        ComboBox<Currency> currencyCombo = new ComboBox<>();
        currencyCombo.getItems().addAll(Currency.values());
        currencyCombo.getSelectionModel().selectFirst();
        ComboBox<Type> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(Type.values());
        typeCombo.getSelectionModel().selectFirst();

        grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Cost:"), 0, 1); grid.add(costField, 1, 1);
        grid.add(new Label("Currency:"), 0, 2); grid.add(currencyCombo, 1, 2);
        grid.add(new Label("Type:"), 0, 3); grid.add(typeCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText();
                    float cost = Float.parseFloat(costField.getText());
                    Currency currency = currencyCombo.getValue();
                    Type type = typeCombo.getValue();
                    return new Expense(expenseObservableList.size() + 1, name, cost, currency, type);
                } catch (Exception ex) {
                    // Optionally show error
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(expense -> {
            try {
                activity.addExpense(expense);
                expenseObservableList.setAll(activity.getExpenses());
                //add tripmanager code
                if (tripManager != null) {
                    tripManager.saveToFile();
                }
            } catch (Exception e) {
                // Optionally show error
            }
        });
    }
}
