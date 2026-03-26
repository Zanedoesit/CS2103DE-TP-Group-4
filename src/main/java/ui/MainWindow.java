package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import trip.Trip;
import trip.TripManager;
import activity.Activity;
import expense.Expense;
import exceptions.TripNotFoundException;
import exceptions.ActivityNotFoundException;
import exceptions.ExpenseNotFoundException;
import exceptions.TimeIntervalConflictException;

public class MainWindow {
	@FXML
	private ListView<Trip> tripListView;
	@FXML
	private ListView<Activity> activityListView;
	@FXML
	private ListView<Expense> expenseListView;
	@FXML
	private Button addTripButton;
	@FXML
	private Button deleteTripButton;
	@FXML
	private Button addActivityButton;
	@FXML
	private Button deleteActivityButton;
	@FXML
	private Button addExpenseButton;
	@FXML
	private Button deleteExpenseButton;

	private TripManager tripManager = new TripManager();
	private ObservableList<Trip> tripObservableList = FXCollections.observableArrayList();
	private ObservableList<Activity> activityObservableList = FXCollections.observableArrayList();
	private ObservableList<Expense> expenseObservableList = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		tripListView.setItems(tripObservableList);
		activityListView.setItems(activityObservableList);
		expenseListView.setItems(expenseObservableList);

		addTripButton.setOnAction(e -> handleAddTrip());
		deleteTripButton.setOnAction(e -> handleDeleteTrip());
		addActivityButton.setOnAction(e -> handleAddActivity());
		deleteActivityButton.setOnAction(e -> handleDeleteActivity());
		addExpenseButton.setOnAction(e -> handleAddExpense());
		deleteExpenseButton.setOnAction(e -> handleDeleteExpense());

		tripListView.getSelectionModel().selectedItemProperty().addListener((obs, oldTrip, newTrip) -> {
			if (newTrip != null) {
				activityObservableList.setAll(newTrip.getActivities());
				expenseObservableList.setAll(newTrip.getExpenses());
			} else {
				activityObservableList.clear();
				expenseObservableList.clear();
			}
		});

		activityListView.getSelectionModel().selectedItemProperty().addListener((obs, oldActivity, newActivity) -> {
			if (newActivity != null) {
				expenseObservableList.setAll(newActivity.getExpenses());
			} else if (tripListView.getSelectionModel().getSelectedItem() != null) {
				expenseObservableList.setAll(tripListView.getSelectionModel().getSelectedItem().getExpenses());
			} else {
				expenseObservableList.clear();
			}
		});
	}

	private void handleAddTrip() {
		// TODO: Show dialog to input trip details
		// For demo, add a dummy trip
		Trip trip = new Trip(tripObservableList.size() + 1, "Trip " + (tripObservableList.size() + 1), java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(3));
		try {
			tripManager.addTrip(trip);
			tripObservableList.setAll(tripManager.getTrips());
		} catch (TimeIntervalConflictException e) {
			showError("Trip time conflict: " + e.getMessage());
		}
	}

	private void handleDeleteTrip() {
		Trip selected = tripListView.getSelectionModel().getSelectedItem();
		if (selected != null) {
			try {
				tripManager.deleteTripById(selected.getId());
				tripObservableList.setAll(tripManager.getTrips());
				activityObservableList.clear();
				expenseObservableList.clear();
			} catch (Exception e) {
				showError("Failed to delete trip: " + e.getMessage());
			}
		}
	}

	private void handleAddActivity() {
		Trip selectedTrip = tripListView.getSelectionModel().getSelectedItem();
		if (selectedTrip != null) {
			Activity activity = new Activity(activityObservableList.size() + 1, "Activity " + (activityObservableList.size() + 1), java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusHours(2));
			try {
				selectedTrip.addActivity(activity);
				activityObservableList.setAll(selectedTrip.getActivities());
			} catch (TimeIntervalConflictException e) {
				showError("Activity time conflict: " + e.getMessage());
			}
		}
	}

	private void handleDeleteActivity() {
		Trip selectedTrip = tripListView.getSelectionModel().getSelectedItem();
		Activity selectedActivity = activityListView.getSelectionModel().getSelectedItem();
		if (selectedTrip != null && selectedActivity != null) {
			try {
				selectedTrip.deleteActivityById(selectedActivity.getId());
				activityObservableList.setAll(selectedTrip.getActivities());
				expenseObservableList.clear();
			} catch (ActivityNotFoundException e) {
				showError("Failed to delete activity: " + e.getMessage());
			}
		}
	}

	private void handleAddExpense() {
		Trip selectedTrip = tripListView.getSelectionModel().getSelectedItem();
		Activity selectedActivity = activityListView.getSelectionModel().getSelectedItem();
		Expense expense = new Expense(expenseObservableList.size() + 1, "Expense " + (expenseObservableList.size() + 1), 100.0f, Expense.Currency.USD, Expense.Type.OTHER);
		if (selectedActivity != null) {
			selectedActivity.addExpense(expense);
			expenseObservableList.setAll(selectedActivity.getExpenses());
		} else if (selectedTrip != null) {
			selectedTrip.addExpense(expense);
			expenseObservableList.setAll(selectedTrip.getExpenses());
		}
	}

	private void handleDeleteExpense() {
		Trip selectedTrip = tripListView.getSelectionModel().getSelectedItem();
		Activity selectedActivity = activityListView.getSelectionModel().getSelectedItem();
		Expense selectedExpense = expenseListView.getSelectionModel().getSelectedItem();
		if (selectedExpense == null) return;
		try {
			if (selectedActivity != null) {
				selectedActivity.deleteExpenseById(selectedExpense.getId());
				expenseObservableList.setAll(selectedActivity.getExpenses());
			} else if (selectedTrip != null) {
				selectedTrip.deleteExpenseById(selectedExpense.getId());
				expenseObservableList.setAll(selectedTrip.getExpenses());
			}
		} catch (ExpenseNotFoundException e) {
			showError("Failed to delete expense: " + e.getMessage());
		}
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
