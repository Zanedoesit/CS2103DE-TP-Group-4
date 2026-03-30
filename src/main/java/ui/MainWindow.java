package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.util.StringConverter;
import javafx.scene.control.DatePicker;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
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
		private BorderPane rootPane;
	// Demo static locations
	private final List<location.Location> locationList = Arrays.asList(
		new location.Location(1, "Singapore", "1 Main St", "Singapore", "Singapore", 1.3521, 103.8198),
		new location.Location(2, "Tokyo", "2 Chiyoda", "Tokyo", "Japan", 35.6895, 139.6917),
		new location.Location(3, "London", "3 Westminster", "London", "UK", 51.5074, -0.1278)
	);
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
		//load any previously saved trips from data/trips.json
		try {
			tripManager.loadFromFile();
		} catch (java.io.IOException e) {
			System.err.println("Could not load saved trips: " + e.getMessage());
		}
		showHomePage();
	}

	public void showHomePage() {
		// Set up the homepage (trip list) in the center pane
		tripObservableList.setAll(tripManager.getTrips());
		tripListView.setItems(tripObservableList);
		// Remove activities and expenses lists from home screen
		// Set custom cell factory for tripListView
		tripListView.setCellFactory(list -> new ListCell<Trip>() {
			@Override
			protected void updateItem(Trip trip, boolean empty) {
				super.updateItem(trip, empty);
				if (empty || trip == null) {
					setText(null);
				} else {
					setText(trip.getName() + " (" + (trip.getLocation() != null ? trip.getLocation().getName() : "No Location") + ") " +
							trip.getStartDateTime().toLocalDate() + " - " + trip.getEndDateTime().toLocalDate());
				}
			}
		});
		addTripButton.setVisible(true);
		deleteTripButton.setVisible(true);
		rootPane.setCenter(tripListView.getParent().getParent());
		addTripButton.setOnAction(e -> handleAddTrip());
		deleteTripButton.setOnAction(e -> handleDeleteTrip());
		tripListView.setOnMouseClicked(event -> {
			Trip selected = tripListView.getSelectionModel().getSelectedItem();
			if (selected != null && event.getClickCount() == 2) {
				showTripPage(selected);
			}
		});
	}

	public void showTripPage(Trip trip) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/TripPage.fxml"));
			BorderPane tripPage = loader.load();
			TripPage controller = loader.getController();
			controller.setTrip(trip);
			controller.setMainWindow(this);
			controller.setTripManager(tripManager);
			rootPane.setCenter(tripPage);
		} catch (Exception e) {
			showError("Failed to load trip page: " + e.getMessage());
		}
	}

	public void showActivityPage(Activity activity, TripPage tripPage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ActivityPage.fxml"));
			BorderPane activityPage = loader.load();
			ActivityPage controller = loader.getController();
			controller.setActivity(activity);
			controller.setTripPage(tripPage);
			controller.setTripManager(tripManager);
			rootPane.setCenter(activityPage);
		} catch (Exception e) {
			showError("Failed to load activity page: " + e.getMessage());
		}
	}


	private void handleAddTrip() {
		Dialog<Trip> dialog = new Dialog<>();
		dialog.setTitle("Add Trip");
		dialog.setHeaderText("Enter trip details");
		ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		TextField nameField = new TextField();
		DatePicker startDatePicker = new DatePicker(LocalDate.now());
		DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(3));
		TextField startTimeField = new TextField("09:00");
		TextField endTimeField = new TextField("18:00");
		ComboBox<location.Location> locationCombo = new ComboBox<>();
		locationCombo.getItems().addAll(locationList);
		locationCombo.setConverter(new StringConverter<location.Location>() {
			@Override public String toString(location.Location l) { return l == null ? "" : l.getName(); }
			@Override public location.Location fromString(String s) { return null; }
		});
		locationCombo.getSelectionModel().selectFirst();

		grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
		grid.add(new Label("Start Date:"), 0, 1); grid.add(startDatePicker, 1, 1);
		grid.add(new Label("Start Time (HH:mm):"), 0, 2); grid.add(startTimeField, 1, 2);
		grid.add(new Label("End Date:"), 0, 3); grid.add(endDatePicker, 1, 3);
		grid.add(new Label("End Time (HH:mm):"), 0, 4); grid.add(endTimeField, 1, 4);
		grid.add(new Label("Location:"), 0, 5); grid.add(locationCombo, 1, 5);

		dialog.getDialogPane().setContent(grid);
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == addButtonType) {
				try {
					String name = nameField.getText();
					LocalDate startDate = startDatePicker.getValue();
					LocalDate endDate = endDatePicker.getValue();
					LocalTime startTime = null;
					LocalTime endTime = null;
					try {
						startTime = LocalTime.parse(startTimeField.getText());
					} catch (Exception e) {
						startTime = LocalTime.of(0, 0);
					}
					try {
						endTime = LocalTime.parse(endTimeField.getText());
					} catch (Exception e) {
						endTime = LocalTime.of(23, 59);
					}
					LocalDateTime start = (startDate != null) ? LocalDateTime.of(startDate, startTime) : null;
					LocalDateTime end = (endDate != null) ? LocalDateTime.of(endDate, endTime) : null;
					location.Location loc = locationCombo.getValue();
					return new Trip(tripObservableList.size() + 1, name, start, end, loc);
				} catch (Exception ex) {
					showError("Invalid input: " + ex.getMessage());
				}
			}
			return null;
		});
		dialog.showAndWait().ifPresent(trip -> {
			try {
				tripManager.addTrip(trip);
				tripManager.saveToFile();
				tripObservableList.setAll(tripManager.getTrips());
			} catch (TimeIntervalConflictException e) {
				showError("Trip time conflict: " + e.getMessage());
			} catch (java.io.IOException e) {
				showError("Failed to save: " + e.getMessage());
			}
		});
	}

	private void handleDeleteTrip() {
		Trip selected = tripListView.getSelectionModel().getSelectedItem();
		if (selected != null) {
			try {
				tripManager.deleteTripById(selected.getId());
				tripManager.saveToFile();
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
		if (selectedTrip == null) return;
		Dialog<Activity> dialog = new Dialog<>();
		dialog.setTitle("Add Activity");
		dialog.setHeaderText("Enter activity details");
		ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		TextField nameField = new TextField();
		DatePicker startDatePicker = new DatePicker(selectedTrip.getStartDateTime().toLocalDate());
		DatePicker endDatePicker = new DatePicker(selectedTrip.getEndDateTime().toLocalDate());
		TextField startTimeField = new TextField("10:00");
		TextField endTimeField = new TextField("12:00");
		ComboBox<location.Location> locationCombo = new ComboBox<>();
		locationCombo.getItems().addAll(locationList);
		locationCombo.setConverter(new StringConverter<location.Location>() {
			@Override public String toString(location.Location l) { return l == null ? "" : l.getName(); }
			@Override public location.Location fromString(String s) { return null; }
		});
		locationCombo.getSelectionModel().selectFirst();

		grid.add(new Label("Name:"), 0, 0); grid.add(nameField, 1, 0);
		grid.add(new Label("Start Date:"), 0, 1); grid.add(startDatePicker, 1, 1);
		grid.add(new Label("Start Time (HH:mm):"), 0, 2); grid.add(startTimeField, 1, 2);
		grid.add(new Label("End Date:"), 0, 3); grid.add(endDatePicker, 1, 3);
		grid.add(new Label("End Time (HH:mm):"), 0, 4); grid.add(endTimeField, 1, 4);
		grid.add(new Label("Location:"), 0, 5); grid.add(locationCombo, 1, 5);

		dialog.getDialogPane().setContent(grid);
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == addButtonType) {
				try {
					String name = nameField.getText();
					LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), LocalTime.parse(startTimeField.getText()));
					LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), LocalTime.parse(endTimeField.getText()));
					location.Location loc = locationCombo.getValue();
					return new Activity(activityObservableList.size() + 1, name, start, end, loc);
				} catch (Exception ex) {
					showError("Invalid input: " + ex.getMessage());
				}
			}
			return null;
		});
		dialog.showAndWait().ifPresent(activity -> {
			try {
				selectedTrip.addActivity(activity);
				tripManager.saveToFile();
				activityObservableList.setAll(selectedTrip.getActivities());
			} catch (TimeIntervalConflictException e) {
				showError("Activity time conflict: " + e.getMessage());
			} catch (java.io.IOException e) {
				showError("Failed to save: " + e.getMessage());
			}
		});
	}

	private void handleDeleteActivity() {
		Trip selectedTrip = tripListView.getSelectionModel().getSelectedItem();
		Activity selectedActivity = activityListView.getSelectionModel().getSelectedItem();
		if (selectedTrip != null && selectedActivity != null) {
			try {
				selectedTrip.deleteActivityById(selectedActivity.getId());
				tripManager.saveToFile();
				activityObservableList.setAll(selectedTrip.getActivities());
				expenseObservableList.clear();
			} catch (ActivityNotFoundException e) {
				showError("Failed to delete activity: " + e.getMessage());
			} catch (java.io.IOException e) {
				showError("Failed to save: " + e.getMessage());
			}
		}
	}

	private void handleAddExpense() {
		Trip selectedTrip = tripListView.getSelectionModel().getSelectedItem();
		if (selectedTrip == null) return;
		Activity selectedActivity = activityListView.getSelectionModel().getSelectedItem();
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
		ComboBox<Expense.Currency> currencyCombo = new ComboBox<>();
		currencyCombo.getItems().addAll(Expense.Currency.values());
		currencyCombo.getSelectionModel().selectFirst();
		ComboBox<Expense.Type> typeCombo = new ComboBox<>();
		typeCombo.getItems().addAll(Expense.Type.values());
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
					Expense.Currency currency = currencyCombo.getValue();
					Expense.Type type = typeCombo.getValue();
					return new Expense(expenseObservableList.size() + 1, name, cost, currency, type);
				} catch (Exception ex) {
					showError("Invalid input: " + ex.getMessage());
				}
			}
			return null;
		});
		dialog.showAndWait().ifPresent(expense -> {
			if (selectedActivity != null) {
				selectedActivity.addExpense(expense);
				expenseObservableList.setAll(selectedActivity.getExpenses());
			} else {
				selectedTrip.addExpense(expense);
				expenseObservableList.setAll(selectedTrip.getExpenses());
			}
			try {
				tripManager.saveToFile();
			} catch (java.io.IOException e) {
				showError("Failed to save: " + e.getMessage());
			}
		});
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
			tripManager.saveToFile();
		} catch (ExpenseNotFoundException e) {
			showError("Failed to delete expense: " + e.getMessage());
		} catch (java.io.IOException e) {
			showError("Failed to save: " + e.getMessage());
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
