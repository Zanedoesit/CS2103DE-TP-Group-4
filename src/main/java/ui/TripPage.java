package ui;

import activity.Activity;
import expense.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import trip.Trip;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import filter.ActivityFilter;

public class TripPage {
    private static final double DAY_TIMELINE_WIDTH = 240.0;
    private static final double BLOCK_HEIGHT = 28.0;
    private static final double LANE_GAP = 8.0;
    private static final double MIN_BLOCK_WIDTH = 16.0;
    private static final DateTimeFormatter DAY_HEADER_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label tripNameLabel;
    @FXML
    private Label tripDateLabel;
    @FXML
    private Label tripLocationLabel;
    @FXML
    private VBox timelineContainer;
    @FXML
    private ComboBox<String> activityTypeFilter;
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
    @FXML
    private Label totalCostLabel;

    private Trip trip;
    private final ObservableList<Activity> activityObservableList = FXCollections.observableArrayList();
    private final ObservableList<Expense> expenseObservableList = FXCollections.observableArrayList();
    private MainWindow mainWindow;
    private trip.TripManager tripManager;

    public void setTrip(Trip trip) {
        this.trip = trip;
        tripNameLabel.setText(trip.getName());
        tripDateLabel.setText(trip.getStartDateTime() + " - " + trip.getEndDateTime());
        tripLocationLabel.setText(trip.getLocation() != null ? trip.getLocation().toString() : "");
        activityObservableList.setAll(trip.getActivities());
        expenseObservableList.setAll(trip.getExpenses());
        refreshTimeline();
        refreshTotalCost();
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void setTripManager(trip.TripManager tripManager) {
        this.tripManager = tripManager;
    }

    @FXML
    private void initialize() {
        activityListView.setItems(activityObservableList);
        expenseListView.setItems(expenseObservableList);

        activityListView.setCellFactory(list -> new ListCell<Activity>() {
            @Override
            protected void updateItem(Activity activity, boolean empty) {
                super.updateItem(activity, empty);
                if (empty || activity == null) {
                    setText(null);
                } else {
                    setText(activity.getName() + " (" + (activity.getLocation() != null ? activity.getLocation().getName() : "No Location") + ") "
                            + activity.getStartDateTime().toLocalDate() + " - " + activity.getEndDateTime().toLocalDate());
                }
            }
        });

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
            if (mainWindow != null) {
                mainWindow.showHomePage();
            }
        });

        //Populate and wire activity type filter
        activityTypeFilter.getItems().add("ALL");
        for (Activity.Type type : Activity.Type.values()) {
            activityTypeFilter.getItems().add(type.name());
        }

        activityTypeFilter.getSelectionModel().selectFirst();
        activityTypeFilter.setOnAction(e -> applyActivityFilter());

        addActivityButton.setOnAction(e -> handleAddActivity());
        addExpenseButton.setOnAction(e -> handleAddExpense());

        activityListView.setOnMouseClicked(event -> {
            Activity selected = activityListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2 && mainWindow != null) {
                mainWindow.showActivityPage(selected, this);
            }
        });
    }

    private void refreshTimeline() {
        timelineContainer.getChildren().clear();
        if (trip == null) {
            return;
        }

        List<Activity> activities = new ArrayList<>(trip.getActivities());
        activities.sort(Comparator.comparing(Activity::getStartDateTime));
        Set<Activity> overlappingActivities = findOverlappingActivities(activities);

        LocalDate day = trip.getStartDateTime().toLocalDate();
        LocalDate tripEndDay = trip.getEndDateTime().toLocalDate();
        while (!day.isAfter(tripEndDay)) {
            List<DaySegment> segments = buildSegmentsForDay(day, activities, overlappingActivities);
            timelineContainer.getChildren().add(buildDayTimelineSection(day, segments));
            day = day.plusDays(1);
        }
    }

    private VBox buildDayTimelineSection(LocalDate day, List<DaySegment> segments) {
        VBox dayBox = new VBox(6);
        dayBox.setStyle("-fx-background-color: #fafafa; -fx-border-color: #d1d5db; -fx-border-radius: 6; "
                + "-fx-background-radius: 6; -fx-padding: 8;");

        Label dayHeader = new Label(day.format(DAY_HEADER_FORMAT));
        dayHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        Pane ruler = createHourRuler();
        Pane timelinePane = createTimelinePane(segments);

        dayBox.getChildren().addAll(dayHeader, ruler, timelinePane);
        return dayBox;
    }

    private Pane createHourRuler() {
        Pane ruler = new Pane();
        ruler.setPrefWidth(DAY_TIMELINE_WIDTH);
        ruler.setMinHeight(18);
        ruler.setPrefHeight(18);

        for (int hour = 0; hour <= 24; hour += 6) {
            Label marker = new Label(String.format("%02d:00", hour));
            marker.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
            marker.setLayoutX(Math.max(0, minutesToPixels(hour * 60) - 14));
            marker.setLayoutY(0);
            ruler.getChildren().add(marker);
        }
        return ruler;
    }

    private Pane createTimelinePane(List<DaySegment> segments) {
        Pane timelinePane = new Pane();
        timelinePane.setPrefWidth(DAY_TIMELINE_WIDTH);
        timelinePane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; "
                + "-fx-border-radius: 4; -fx-background-radius: 4;");

        int laneCount = assignLanes(segments);
        double timelineHeight = Math.max(44.0, 8.0 + laneCount * (BLOCK_HEIGHT + LANE_GAP));
        timelinePane.setMinHeight(timelineHeight);
        timelinePane.setPrefHeight(timelineHeight);

        addHourGridLines(timelinePane);

        if (segments.isEmpty()) {
            Label emptyState = new Label("No activities planned");
            emptyState.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            emptyState.setLayoutX(10);
            emptyState.setLayoutY(12);
            timelinePane.getChildren().add(emptyState);
            return timelinePane;
        }

        for (DaySegment segment : segments) {
            Label activityBlock = new Label(createBlockText(segment));
            activityBlock.setAlignment(Pos.CENTER_LEFT);
            activityBlock.setPrefHeight(BLOCK_HEIGHT);
            activityBlock.setMinHeight(BLOCK_HEIGHT);

            double x = minutesToPixels(segment.startMinute);
            double width = Math.max(MIN_BLOCK_WIDTH, minutesToPixels(segment.endMinute - segment.startMinute));
            double maxAllowedWidth = Math.max(4.0, DAY_TIMELINE_WIDTH - x - 1);
            width = Math.min(width, maxAllowedWidth);
            double y = 6 + segment.lane * (BLOCK_HEIGHT + LANE_GAP);

            activityBlock.setLayoutX(x);
            activityBlock.setLayoutY(y);
            activityBlock.setPrefWidth(width);

            if (segment.overlaps) {
                activityBlock.setStyle("-fx-background-color: #fee2e2; -fx-border-color: #ef4444; -fx-border-radius: 4; "
                        + "-fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-text-fill: #7f1d1d;");
            } else {
                activityBlock.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #3b82f6; -fx-border-radius: 4; "
                        + "-fx-background-radius: 4; -fx-padding: 4 8 4 8; -fx-text-fill: #1e3a8a;");
            }

            String warning = segment.overlaps ? " (overlap detected)" : "";
            activityBlock.setTooltip(new Tooltip(segment.activity.getName() + ": "
                    + createTimeRangeText(segment.startMinute, segment.endMinute) + warning));
            timelinePane.getChildren().add(activityBlock);
        }
        return timelinePane;
    }

    private void addHourGridLines(Pane timelinePane) {
        for (int hour = 0; hour <= 24; hour++) {
            Region line = new Region();
            line.setLayoutX(minutesToPixels(hour * 60));
            line.setLayoutY(0);
            line.setPrefWidth(hour % 6 == 0 ? 1.2 : 0.8);
            line.prefHeightProperty().bind(timelinePane.heightProperty());
            if (hour % 6 == 0) {
                line.setStyle("-fx-background-color: #d1d5db;");
            } else {
                line.setStyle("-fx-background-color: #f1f5f9;");
            }
            timelinePane.getChildren().add(line);
        }
    }

    private String createBlockText(DaySegment segment) {
        return segment.activity.getName() + " | " + createTimeRangeText(segment.startMinute, segment.endMinute);
    }

    private String createTimeRangeText(int startMinute, int endMinute) {
        return formatMinuteOfDay(startMinute) + "-" + formatMinuteOfDay(endMinute);
    }

    private String formatMinuteOfDay(int minuteOfDay) {
        if (minuteOfDay >= 24 * 60) {
            return "24:00";
        }
        int normalized = Math.max(0, minuteOfDay);
        return LocalTime.MIN.plusMinutes(normalized).format(TIME_FORMAT);
    }

    private double minutesToPixels(int minutes) {
        return (minutes / 1440.0) * DAY_TIMELINE_WIDTH;
    }

    private int assignLanes(List<DaySegment> segments) {
        List<Integer> laneEnds = new ArrayList<>();
        for (DaySegment segment : segments) {
            int assignedLane = -1;
            for (int lane = 0; lane < laneEnds.size(); lane++) {
                if (segment.startMinute >= laneEnds.get(lane)) {
                    assignedLane = lane;
                    laneEnds.set(lane, segment.endMinute);
                    break;
                }
            }
            if (assignedLane == -1) {
                laneEnds.add(segment.endMinute);
                assignedLane = laneEnds.size() - 1;
            }
            segment.lane = assignedLane;
        }
        return laneEnds.size();
    }

    private List<DaySegment> buildSegmentsForDay(LocalDate day, List<Activity> activities, Set<Activity> overlappingActivities) {
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

        List<DaySegment> segments = new ArrayList<>();
        for (Activity activity : activities) {
            LocalDateTime clippedStart = activity.getStartDateTime().isAfter(dayStart) ? activity.getStartDateTime() : dayStart;
            LocalDateTime clippedEnd = activity.getEndDateTime().isBefore(dayEnd) ? activity.getEndDateTime() : dayEnd;
            if (clippedEnd.isAfter(clippedStart)) {
                int startMinute = (int) Duration.between(dayStart, clippedStart).toMinutes();
                int endMinute = (int) Duration.between(dayStart, clippedEnd).toMinutes();
                segments.add(new DaySegment(activity, startMinute, endMinute, overlappingActivities.contains(activity)));
            }
        }
        segments.sort(Comparator
                .comparingInt((DaySegment segment) -> segment.startMinute)
                .thenComparingInt(segment -> segment.endMinute));
        return segments;
    }

    private Set<Activity> findOverlappingActivities(List<Activity> activities) {
        Set<Activity> overlaps = new HashSet<>();
        for (int i = 0; i < activities.size(); i++) {
            Activity current = activities.get(i);
            for (int j = i + 1; j < activities.size(); j++) {
                Activity other = activities.get(j);
                if (current.overlapsWith(other)) {
                    overlaps.add(current);
                    overlaps.add(other);
                }
            }
        }
        return overlaps;
    }

    private void applyActivityFilter() {
        String selected = activityTypeFilter.getSelectionModel().getSelectedItem();
        Activity.Type filterType = null;
        if (selected != null && !"ALL".equals(selected)) {
            filterType = Activity.Type.valueOf(selected);
        }
        List<Activity> filtered = ActivityFilter.byType(trip.getActivities(), filterType);
        activityObservableList.setAll(filtered);
    }

    private void refreshTotalCost() {
        StringBuilder sb = new StringBuilder("Total Cost: ");
        boolean first = true;
        for (Expense.Currency currency : Expense.Currency.values()) {
            float total = trip.getTotalCost(currency);
            if (total > 0) {
                if (!first) {
                    sb.append(" | ");
                }
                sb.append(String.format("%.2f %s", total, currency.name()));
                first = false;
            }
        }
        if (first) {
            sb.append("No expenses yet");
        }
        totalCostLabel.setText(sb.toString());
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
        ComboBox<Expense.Currency> currencyCombo = new ComboBox<>();
        currencyCombo.getItems().addAll(Expense.Currency.values());
        currencyCombo.getSelectionModel().selectFirst();
        ComboBox<Expense.Type> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(Expense.Type.values());
        typeCombo.getSelectionModel().selectFirst();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Cost:"), 0, 1);
        grid.add(costField, 1, 1);
        grid.add(new Label("Currency:"), 0, 2);
        grid.add(currencyCombo, 1, 2);
        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeCombo, 1, 3);

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
                    // Intentionally returns null for invalid input.
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(expense -> {
            trip.addExpense(expense);
            expenseObservableList.setAll(trip.getExpenses());
            refreshTotalCost();
            if (tripManager != null) {
                try {
                    tripManager.saveToFile();
                } catch (java.io.IOException e) {
                    // Intentionally ignored in this lightweight UI flow.
                }
            }
        });
    }

    private void handleAddActivity() {
        Dialog<Activity> dialog = new Dialog<>();
        dialog.setTitle("Add Activity");
        dialog.setHeaderText("Enter activity details");
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        LocalDate tripStart = trip.getStartDateTime().toLocalDate();
        LocalDate tripEnd = trip.getEndDateTime().toLocalDate();
        DatePicker startDatePicker = new DatePicker(tripStart);
        DatePicker endDatePicker = new DatePicker(tripEnd);
        TextField startTimeField = new TextField("00:00");
        TextField endTimeField = new TextField("23:59");
        ComboBox<location.Location> locationCombo = new ComboBox<>();
        locationCombo.getItems().add(trip.getLocation());
        locationCombo.getSelectionModel().selectFirst();

        ComboBox<Activity.Type> activityTypeCombo = new ComboBox<>();
        activityTypeCombo.getItems().addAll(Activity.Type.values());
        activityTypeCombo.getSelectionModel().selectFirst();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Start Date:"), 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(new Label("Start Time (HH:mm):"), 0, 2);
        grid.add(startTimeField, 1, 2);
        grid.add(new Label("End Date:"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(new Label("End Time (HH:mm):"), 0, 4);
        grid.add(endTimeField, 1, 4);
        grid.add(new Label("Location:"), 0, 5);
        grid.add(locationCombo, 1, 5);

        grid.add(new Label("Type:"), 0, 6);
        grid.add(activityTypeCombo, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText();
                    LocalDate startDate = startDatePicker.getValue();
                    LocalDate endDate = endDatePicker.getValue();
                    LocalTime startTime;
                    LocalTime endTime;
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
                   // return new Activity(activityObservableList.size() + 1, name, start, end, loc);
                    Activity newActivity = new Activity(activityObservableList.size() + 1, name, start, end, loc);
                    newActivity.addType(activityTypeCombo.getValue());
                    return newActivity;
                } catch (Exception ex) {
                    // Intentionally returns null for invalid input.
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(activity -> {
            try {
                trip.addActivity(activity);
              //  activityObservableList.setAll(trip.getActivities());
                applyActivityFilter();
                refreshTimeline();
                refreshTotalCost();
                if (tripManager != null) {
                    tripManager.saveToFile();
                }
            } catch (Exception e) {
                // Intentionally ignored in this lightweight UI flow.
            }
        });
    }

    public void showTripPage() {
        if (mainWindow != null && trip != null) {
            mainWindow.showTripPage(trip);
        }
    }

    private static class DaySegment {
        private final Activity activity;
        private final int startMinute;
        private final int endMinute;
        private final boolean overlaps;
        private int lane;

        private DaySegment(Activity activity, int startMinute, int endMinute, boolean overlaps) {
            this.activity = activity;
            this.startMinute = startMinute;
            this.endMinute = endMinute;
            this.overlaps = overlaps;
        }
    }
}
