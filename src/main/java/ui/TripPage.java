package ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import trip.Trip;
import activity.Activity;
import expense.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;

public class TripPage {
    @FXML
    private Label tripNameLabel;
    @FXML
    private Label tripDateLabel;
    @FXML
    private Label tripLocationLabel;
    @FXML
    private ListView<Activity> activityListView;
    @FXML
    private ListView<Expense> expenseListView;
    @FXML
    private Button addActivityButton;
    @FXML
    private Button addExpenseButton;
    @FXML
    private Button backButton;

    private Trip trip;
    private ObservableList<Activity> activityObservableList = FXCollections.observableArrayList();
    private ObservableList<Expense> expenseObservableList = FXCollections.observableArrayList();
    private MainWindow mainWindow;

    public void setTrip(Trip trip) {
        this.trip = trip;
        tripNameLabel.setText(trip.getName());
        tripDateLabel.setText(trip.getStartDateTime() + " - " + trip.getEndDateTime());
        tripLocationLabel.setText(trip.getLocation() != null ? trip.getLocation().toString() : "");
        activityObservableList.setAll(trip.getActivities());
        expenseObservableList.setAll(trip.getExpenses());
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    @FXML
    private void initialize() {
        activityListView.setItems(activityObservableList);
        expenseListView.setItems(expenseObservableList);
        // Set custom cell factory for activity list
        activityListView.setCellFactory(list -> new ListCell<activity.Activity>() {
            @Override
            protected void updateItem(activity.Activity activity, boolean empty) {
                super.updateItem(activity, empty);
                if (empty || activity == null) {
                    setText(null);
                } else {
                    setText(activity.getName() + " (" + (activity.getLocation() != null ? activity.getLocation().getName() : "No Location") + ") " +
                            activity.getStartDateTime().toLocalDate() + " - " + activity.getEndDateTime().toLocalDate());
                }
            }
        });
        // Set custom cell factory for expense list
        expenseListView.setCellFactory(list -> new ListCell<expense.Expense>() {
            @Override
            protected void updateItem(expense.Expense expense, boolean empty) {
                super.updateItem(expense, empty);
                if (empty || expense == null) {
                    setText(null);
                } else {
                    setText(expense.getName() + " - " + expense.getCost() + " " + expense.getCurrency() + " (" + expense.getType() + ")");
                }
            }
        });
        backButton.setOnAction(e -> {
            if (mainWindow != null) {
                mainWindow.showHomePage();
            }
        });

        addActivityButton.setOnAction(e -> handleAddActivity());
        addExpenseButton.setOnAction(e -> handleAddExpense());

        activityListView.setOnMouseClicked(event -> {
            Activity selected = activityListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2 && mainWindow != null) {
                mainWindow.showActivityPage(selected, this);
            }
        });

        activityListView.setOnMouseClicked(event -> {
            Activity selected = activityListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2 && mainWindow != null) {
                mainWindow.showActivityPage(selected, this);
            }
        });
    }


    private void handleAddExpense() {
        javafx.scene.control.Dialog<Expense> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Add Expense");
        dialog.setHeaderText("Enter expense details");
        javafx.scene.control.ButtonType addButtonType = new javafx.scene.control.ButtonType("Add", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField costField = new javafx.scene.control.TextField("100.0");
        javafx.scene.control.ComboBox<Expense.Currency> currencyCombo = new javafx.scene.control.ComboBox<>();
        currencyCombo.getItems().addAll(Expense.Currency.values());
        currencyCombo.getSelectionModel().selectFirst();
        javafx.scene.control.ComboBox<Expense.Type> typeCombo = new javafx.scene.control.ComboBox<>();
        typeCombo.getItems().addAll(Expense.Type.values());
        typeCombo.getSelectionModel().selectFirst();

        grid.add(new javafx.scene.control.Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Cost:"), 0, 1); grid.add(costField, 1, 1);
        grid.add(new javafx.scene.control.Label("Currency:"), 0, 2); grid.add(currencyCombo, 1, 2);
        grid.add(new javafx.scene.control.Label("Type:"), 0, 3); grid.add(typeCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText();
                    float cost = Float.parseFloat(costField.getText());
                    Expense.Currency currency = currencyCombo.getValue();
                    Expense.Type type = typeCombo.getValue();
                    return new Expense(expenseObservableList.size() + 1, name, cost, currency, type);
                } catch (Exception ex) {
                    // Optionally show error dialog here
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(expense -> {

            trip.addExpense(expense);
            expenseObservableList.setAll(trip.getExpenses());
        });
    }

    private void handleAddActivity() {
        javafx.scene.control.Dialog<Activity> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Add Activity");
        dialog.setHeaderText("Enter activity details");
        javafx.scene.control.ButtonType addButtonType = new javafx.scene.control.ButtonType("Add", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        java.time.LocalDate tripStart = trip.getStartDateTime().toLocalDate();
        java.time.LocalDate tripEnd = trip.getEndDateTime().toLocalDate();
        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(tripStart);
        javafx.scene.control.DatePicker endDatePicker = new javafx.scene.control.DatePicker(tripEnd);
        javafx.scene.control.TextField startTimeField = new javafx.scene.control.TextField("00:00");
        javafx.scene.control.TextField endTimeField = new javafx.scene.control.TextField("23:59");
        javafx.scene.control.ComboBox<location.Location> locationCombo = new javafx.scene.control.ComboBox<>();
        locationCombo.getItems().add(trip.getLocation());
        locationCombo.getSelectionModel().selectFirst();

        grid.add(new javafx.scene.control.Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Start Date:"), 0, 1); grid.add(startDatePicker, 1, 1);
        grid.add(new javafx.scene.control.Label("Start Time (HH:mm):"), 0, 2); grid.add(startTimeField, 1, 2);
        grid.add(new javafx.scene.control.Label("End Date:"), 0, 3); grid.add(endDatePicker, 1, 3);
        grid.add(new javafx.scene.control.Label("End Time (HH:mm):"), 0, 4); grid.add(endTimeField, 1, 4);
        grid.add(new javafx.scene.control.Label("Location:"), 0, 5); grid.add(locationCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText();
                    java.time.LocalDate startDate = startDatePicker.getValue();
                    java.time.LocalDate endDate = endDatePicker.getValue();
                    java.time.LocalTime startTime;
                    java.time.LocalTime endTime;
                    try {
                        startTime = java.time.LocalTime.parse(startTimeField.getText());
                    } catch (Exception e) {
                        startTime = java.time.LocalTime.of(0, 0);
                    }
                    try {
                        endTime = java.time.LocalTime.parse(endTimeField.getText());
                    } catch (Exception e) {
                        endTime = java.time.LocalTime.of(23, 59);
                    }
                    java.time.LocalDateTime start = (startDate != null) ? java.time.LocalDateTime.of(startDate, startTime) : null;
                    java.time.LocalDateTime end = (endDate != null) ? java.time.LocalDateTime.of(endDate, endTime) : null;
                    location.Location loc = locationCombo.getValue();
                    return new Activity(activityObservableList.size() + 1, name, start, end, loc);
                } catch (Exception ex) {
                    // Optionally show error
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(activity -> {
            try {
                trip.addActivity(activity);
                activityObservableList.setAll(trip.getActivities());
            } catch (Exception e) {
                // Optionally show error
            }
        });
    }

    public void showTripPage() {
        if (mainWindow != null && trip != null) {
            mainWindow.showTripPage(trip);
        }
    }
}
