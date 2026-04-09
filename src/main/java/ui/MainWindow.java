package ui;

import activity.Activity;
import country.Country;
import country.CountryRepository;
import expense.ExpenseRepository;
import exceptions.TimeIntervalConflictException;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import location.Location;
import location.LocationRepository;
import storage.ImageAssetStore;
import trip.Trip;
import trip.TripManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainWindow {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final Comparator<Trip> TRIP_DISPLAY_COMPARATOR = Comparator
        .comparingInt(Trip::getPriority).reversed()
        .thenComparing(Trip::getStartDateTime)
        .thenComparing(Trip::getEndDateTime);
    private static final Comparator<ActivitySummaryEntry> SNAPSHOT_ACTIVITY_COMPARATOR = Comparator
        .comparingInt((ActivitySummaryEntry entry) -> entry.activity.getPriority()).reversed()
        .thenComparing(entry -> entry.activity.getStartDateTime())
        .thenComparing(entry -> entry.activity.getEndDateTime());

    private enum PageContext {
        HOME,
        TRIP,
        ACTIVITY
    }

    @FXML
    private BorderPane rootPane;
    @FXML
    private ListView<Trip> tripListView;
    @FXML
    private HBox homeContent;
    @FXML
    private Button addTripButton;
    @FXML
    private Button editTripButton;
    @FXML
    private Button deleteTripButton;
    @FXML
    private Button helpButton;
    @FXML
    private VBox ongoingActivityContainer;
    @FXML
    private VBox upcomingActivityContainer;

    private final TripManager tripManager = new TripManager();
    private final CountryRepository countryRepository = new CountryRepository();
    private final LocationRepository locationRepository = new LocationRepository(countryRepository);
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final ImageAssetStore imageAssetStore = new ImageAssetStore();
    private final ObservableList<Trip> tripObservableList = FXCollections.observableArrayList();
    private PageContext currentPageContext = PageContext.HOME;

    @FXML
    public void initialize() {
        try {
            countryRepository.load();
            locationRepository.load();
            expenseRepository.load();
            tripManager.loadFromFile(countryRepository, locationRepository, expenseRepository);
            expenseRepository.save();
            tripManager.saveToFile();
        } catch (IOException | IllegalStateException e) {
            showError("Could not load saved data: " + e.getMessage());
        }

        showHomePage();

        if (helpButton != null) {
            helpButton.setOnAction(e -> showGuideForCurrentPage());
        }
        refreshHeaderActivitySummary();
    }

    public void showHomePage() {
        currentPageContext = PageContext.HOME;
        refreshTripList();
        tripListView.setItems(tripObservableList);
        tripListView.setPlaceholder(new Label("No trips yet. Click Add Trip to begin."));
        tripListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Trip trip, boolean empty) {
                super.updateItem(trip, empty);
                if (empty || trip == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                ImageView imageView = new ImageView();
                imageView.setFitHeight(52);
                imageView.setFitWidth(52);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("thumb");
                Image image = resolveImage(trip.getCountry() != null ? trip.getCountry().getImagePath() : null);
                if (image != null) {
                    imageView.setImage(image);
                }

                Label title = new Label(trip.getName());
                title.getStyleClass().add("cell-title");

                Label subtitle = new Label(formatDateTimeRange(trip.getStartDateTime(), trip.getEndDateTime()));
                subtitle.getStyleClass().add("cell-subtitle");

                String countryText = trip.getCountry() != null ? trip.getCountry().getName() : "No country";
                Label countryMeta = new Label("Country: " + countryText);
                countryMeta.getStyleClass().add("cell-meta");
                Label activityMeta = new Label("Activities: " + trip.getActivities().size());
                activityMeta.getStyleClass().add("cell-meta");

                VBox textBox = new VBox(3, title, subtitle, countryMeta, activityMeta);
                HBox card = new HBox(10, imageView, textBox);
                card.getStyleClass().add("friendly-cell");
                card.setMaxWidth(Double.MAX_VALUE);

                setText(null);
                setGraphic(card);
            }
        });

        addTripButton.setOnAction(e -> handleAddTrip());
        if (editTripButton != null) {
            editTripButton.setOnAction(e -> handleEditTrip());
        }
        deleteTripButton.setOnAction(e -> handleDeleteTrip());
        tripListView.setOnMouseClicked(event -> {
            Trip selected = tripListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                showTripPage(selected);
            }
        });

        if (homeContent != null) {
            rootPane.setCenter(homeContent);
        }
        refreshHeaderActivitySummary();
    }

    public void showTripPage(Trip trip) {
        currentPageContext = PageContext.TRIP;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/TripPage.fxml"));
            BorderPane tripPage = loader.load();
            TripPage controller = loader.getController();
            controller.setTrip(trip);
            controller.setMainWindow(this);
            controller.setTripManager(tripManager);
            controller.setExpenseRepository(expenseRepository);
            rootPane.setCenter(tripPage);
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String detail = root.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = root.getClass().getSimpleName();
            }
            e.printStackTrace();
            showError("Failed to load trip page: " + detail);
        }
    }

    public void showActivityPage(Activity activity, TripPage tripPage) {
        currentPageContext = PageContext.ACTIVITY;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ActivityPage.fxml"));
            BorderPane activityPage = loader.load();
            ActivityPage controller = loader.getController();
            controller.setActivity(activity);
            controller.setTripPage(tripPage);
            controller.setTripManager(tripManager);
            controller.setMainWindow(this);
            controller.setExpenseRepository(expenseRepository);
            rootPane.setCenter(activityPage);
        } catch (Exception e) {
            showError("Failed to load activity page: " + e.getMessage());
        }
    }

    public List<Country> getAvailableCountries() {
        return countryRepository.getCountries();
    }

    public List<Location> getAvailableLocations() {
        return locationRepository.getLocations();
    }

    public Country promptAddCountry() {
        Window owner = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        Country country = openAddCountryDialog(owner);
        if (country != null) {
            saveLookupStores();
        }
        return country;
    }

    public Location promptAddLocation() {
        Window owner = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        Location location = openAddLocationDialog(owner);
        if (location != null) {
            saveLookupStores();
        }
        return location;
    }

    public Country promptEditCountry(Country country) {
        if (country == null) {
            showError("Please select a country to edit.");
            return null;
        }
        Window owner = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        Country updated = openEditCountryDialog(owner, country);
        if (updated != null) {
            saveLookupStores();
        }
        return updated;
    }

    public Location promptEditLocation(Location location) {
        if (location == null) {
            showError("Please select a location to edit.");
            return null;
        }
        Window owner = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        Location updated = openEditLocationDialog(owner, location);
        if (updated != null) {
            saveLookupStores();
        }
        return updated;
    }

    public boolean promptEditTrip(Trip trip) {
        if (trip == null) {
            showError("Please select a trip to edit.");
            return false;
        }
        return openEditTripDialog(trip);
    }

    public boolean deleteTripFromUi(Trip trip) {
        if (trip == null) {
            showError("Please select a trip to delete.");
            return false;
        }
        try {
            tripManager.deleteTripById(trip.getId());
            tripManager.saveToFile();
            removeExpensesOwnedByTrip(trip);
            refreshTripList();
            refreshHeaderActivitySummary();
            return true;
        } catch (Exception e) {
            showError("Failed to delete trip: " + e.getMessage());
            return false;
        }
    }

    public void cleanupExpenseIfOrphaned(int expenseId) {
        if (isExpenseReferencedAnywhere(expenseId)) {
            return;
        }
        try {
            expenseRepository.deleteExpenseById(expenseId);
            expenseRepository.save();
        } catch (IllegalArgumentException ignored) {
            // Already removed from repository.
        } catch (Exception e) {
            showError("Failed to clean up expense: " + e.getMessage());
        }
    }

    public boolean deleteCountryFromUi(Country country, Runnable onDataChanged) {
        if (country == null) {
            showError("Please select a country to delete.");
            return false;
        }
        return attemptDeleteCountry(country, onDataChanged);
    }

    public boolean deleteLocationFromUi(Location location, Runnable onDataChanged) {
        if (location == null) {
            showError("Please select a location to delete.");
            return false;
        }
        return attemptDeleteLocation(location, onDataChanged);
    }

    public void showTripGuide() {
        showGuideDialog("Trip Page Guide", "Trip page controls",
            List.of(
                "Use Edit Trip to change trip name, dates, and country.",
                "Use Add Activity to build your itinerary for this trip.",
                "Use Edit Activity to update timing, type, and location.",
                "Use Add Expense, Edit Expense, and Delete Expense to manage costs.",
                "Use the activity filter to focus on one activity type.",
                "Use Back to return to the trips list."
            ));
    }

    public void showActivityGuide() {
        showGuideDialog("Activity Page Guide", "Activity expense controls",
            List.of(
                "Use this page to manage expenses tied to one activity.",
                "Use Add Expense to record new spending.",
                "Use Edit Expense to correct amounts, category, currency, or image.",
                "Use Delete Expense to remove an entry that is no longer needed.",
                "Use Edit Activity to update activity details without leaving this page.",
                "Use Back to return to the trip page."
            ));
    }

    private void showGuideForCurrentPage() {
        switch (currentPageContext) {
        case TRIP:
            showTripGuide();
            break;
        case ACTIVITY:
            showActivityGuide();
            break;
        case HOME:
        default:
            showHomeGuide();
            break;
        }
    }

    private void handleAddTrip() {
        Dialog<Trip> dialog = new Dialog<>();
        dialog.setTitle("Add Trip");
        dialog.setHeaderText("Enter trip details");
        applyDialogTheme(dialog, "form-dialog");
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

        ComboBox<Country> countryCombo = new ComboBox<>();
        Runnable refreshCountries = () -> {
            Country selectedCountry = countryCombo.getValue();
            countryCombo.getItems().setAll(countryRepository.getCountries());
            if (countryCombo.getItems().isEmpty()) {
                countryCombo.getSelectionModel().clearSelection();
                return;
            }
            if (selectedCountry == null || !countryCombo.getItems().contains(selectedCountry)) {
                countryCombo.getSelectionModel().selectFirst();
            }
        };
        refreshCountries.run();
        countryCombo.setConverter(createCountryConverter());
        configureCountryComboForDelete(countryCombo, refreshCountries);
        if (!countryCombo.getItems().isEmpty()) {
            countryCombo.getSelectionModel().selectFirst();
        }

        Button newCountryButton = createAddButton("New...");
        newCountryButton.setOnAction(e -> {
            Country country = openAddCountryDialog(dialog.getDialogPane().getScene().getWindow());
            if (country != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(country);
            }
        });

        Button editCountryButton = createEditButton("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = createDeleteButton("Delete");
        deleteCountryButton.setOnAction(e -> deleteCountryFromUi(countryCombo.getValue(), refreshCountries));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Start Date"), 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(new Label("Start Time (HH:mm)"), 0, 2);
        grid.add(startTimeField, 1, 2);
        grid.add(new Label("End Date"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(new Label("End Time (HH:mm)"), 0, 4);
        grid.add(endTimeField, 1, 4);
        grid.add(new Label("Country"), 0, 5);
        grid.add(createResponsiveActionRow(countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != addButtonType) {
                return null;
            }
            try {
                String name = nameField.getText();
                LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), parseTimeOrDefault(startTimeField.getText(), LocalTime.of(0, 0)));
                LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), parseTimeOrDefault(endTimeField.getText(), LocalTime.of(23, 59)));
                Country country = countryCombo.getValue();
                if (country == null) {
                    throw new IllegalArgumentException("Country is required");
                }
                return new Trip(tripManager.nextAvailableId(), name, start, end, country);
            } catch (Exception ex) {
                showError("Invalid input: " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(trip -> {
            try {
                tripManager.addTrip(trip);
                tripManager.saveToFile();
                refreshTripList();
                refreshHeaderActivitySummary();
            } catch (IllegalArgumentException e) {
                showError("Invalid trip: " + e.getMessage());
            } catch (TimeIntervalConflictException e) {
                showError("Trip time conflict: " + e.getMessage());
            } catch (IOException e) {
                showError("Failed to save: " + e.getMessage());
            }
        });
    }

    private void handleDeleteTrip() {
        Trip selected = tripListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        deleteTripFromUi(selected);
    }

    private void handleEditTrip() {
        Trip selected = tripListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a trip to edit.");
            return;
        }

        openEditTripDialog(selected);
    }

    private boolean openEditTripDialog(Trip selected) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Trip");
        dialog.setHeaderText("Update trip details");
        applyDialogTheme(dialog, "form-dialog");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(selected.getName());
        DatePicker startDatePicker = new DatePicker(selected.getStartDateTime().toLocalDate());
        DatePicker endDatePicker = new DatePicker(selected.getEndDateTime().toLocalDate());
        TextField startTimeField = new TextField(selected.getStartDateTime().toLocalTime().toString());
        TextField endTimeField = new TextField(selected.getEndDateTime().toLocalTime().toString());

        ComboBox<Country> countryCombo = new ComboBox<>();
        Runnable refreshCountries = () -> {
            Country selectedCountry = countryCombo.getValue();
            countryCombo.getItems().setAll(countryRepository.getCountries());
            if (countryCombo.getItems().isEmpty()) {
                countryCombo.getSelectionModel().clearSelection();
                return;
            }
            if (selectedCountry == null || !countryCombo.getItems().contains(selectedCountry)) {
                countryCombo.getSelectionModel().selectFirst();
            }
        };
        refreshCountries.run();
        countryCombo.setConverter(createCountryConverter());
        configureCountryComboForDelete(countryCombo, refreshCountries);
        if (selected.getCountry() != null && countryCombo.getItems().contains(selected.getCountry())) {
            countryCombo.getSelectionModel().select(selected.getCountry());
        }

        Button newCountryButton = createAddButton("New...");
        newCountryButton.setOnAction(e -> {
            Country created = promptAddCountry();
            if (created != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(created);
            }
        });

        Button editCountryButton = createEditButton("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = createDeleteButton("Delete");
        deleteCountryButton.setOnAction(e -> deleteCountryFromUi(countryCombo.getValue(), refreshCountries));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Start Date"), 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(new Label("Start Time (HH:mm)"), 0, 2);
        grid.add(startTimeField, 1, 2);
        grid.add(new Label("End Date"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(new Label("End Time (HH:mm)"), 0, 4);
        grid.add(endTimeField, 1, 4);
        grid.add(new Label("Country"), 0, 5);
        grid.add(createResponsiveActionRow(countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 5);

        dialog.getDialogPane().setContent(grid);
        final boolean[] updated = new boolean[]{false};
        dialog.showAndWait().ifPresent(result -> {
            if (result != saveButtonType) {
                return;
            }
            try {
                Country updatedCountry = countryCombo.getValue();
                if (updatedCountry == null) {
                    throw new IllegalArgumentException("Country is required");
                }
                LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), parseTimeOrDefault(startTimeField.getText(), LocalTime.of(0, 0)));
                LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), parseTimeOrDefault(endTimeField.getText(), LocalTime.of(23, 59)));
                tripManager.updateTrip(selected.getId(), nameField.getText(), start, end, updatedCountry);
                tripManager.saveToFile();
                refreshTripList();
                refreshHeaderActivitySummary();
                updated[0] = true;
            } catch (Exception e) {
                showError("Failed to edit trip: " + e.getMessage());
            }
        });
        return updated[0];
    }

    private void removeExpensesOwnedByTrip(Trip trip) {
        List<Integer> removableExpenseIds = new ArrayList<>();
        for (expense.Expense expense : trip.getExpenses()) {
            removableExpenseIds.add(expense.getId());
        }
        for (Activity activity : trip.getActivities()) {
            for (expense.Expense expense : activity.getExpenses()) {
                removableExpenseIds.add(expense.getId());
            }
        }

        for (Integer expenseId : removableExpenseIds.stream().distinct().toList()) {
            if (isExpenseReferencedAnywhere(expenseId)) {
                continue;
            }
            try {
                expenseRepository.deleteExpenseById(expenseId);
            } catch (IllegalArgumentException ignored) {
                // Expense may already be removed by a previous pass.
            }
        }

        if (!removableExpenseIds.isEmpty()) {
            try {
                expenseRepository.save();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to save expenses after trip deletion", e);
            }
        }
    }

    private boolean isExpenseReferencedAnywhere(int expenseId) {
        for (Trip trip : tripManager.getTrips()) {
            for (expense.Expense expense : trip.getExpenses()) {
                if (expense.getId() == expenseId) {
                    return true;
                }
            }
            for (Activity activity : trip.getActivities()) {
                for (expense.Expense expense : activity.getExpenses()) {
                    if (expense.getId() == expenseId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Country openAddCountryDialog(Window owner) {
        Dialog<Country> dialog = new Dialog<>();
        dialog.setTitle("Add Country");
        dialog.setHeaderText("Enter country details (name required)");
        applyDialogTheme(dialog, "form-dialog");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType addButtonType = new ButtonType("Add Country", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField continentField = new TextField();
        TextField imagePathField = new TextField();
        imagePathField.setEditable(false);
        Button uploadButton = new Button("Upload Image...");
        uploadButton.setOnAction(e -> chooseImagePath(dialog, imagePathField));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Continent"), 0, 1);
        grid.add(continentField, 1, 1);
        grid.add(new Label("Image"), 0, 2);
        grid.add(new HBox(8, imagePathField, uploadButton), 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != addButtonType) {
                return null;
            }
            try {
                return countryRepository.addCountry(nameField.getText(), continentField.getText(), imagePathField.getText());
            } catch (Exception ex) {
                showError("Invalid country: " + ex.getMessage());
                return null;
            }
        });

        return dialog.showAndWait().orElse(null);
    }

    private Country openEditCountryDialog(Window owner, Country country) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Country");
        dialog.setHeaderText("Update country details");
        applyDialogTheme(dialog, "form-dialog");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(country.getName());
        TextField continentField = new TextField(country.getContinent() == null ? "" : country.getContinent());
        TextField imagePathField = new TextField(country.getImagePath() == null ? "" : country.getImagePath());
        imagePathField.setEditable(false);
        Button uploadButton = new Button("Upload Image...");
        uploadButton.setOnAction(e -> chooseImagePath(dialog, imagePathField));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Continent"), 0, 1);
        grid.add(continentField, 1, 1);
        grid.add(new Label("Image"), 0, 2);
        grid.add(new HBox(8, imagePathField, uploadButton), 1, 2);

        dialog.getDialogPane().setContent(grid);
        final boolean[] updated = new boolean[]{false};
        dialog.showAndWait().ifPresent(result -> {
            if (result != saveButtonType) {
                return;
            }
            try {
                countryRepository.updateCountry(country.getId(), nameField.getText(), continentField.getText(), imagePathField.getText());
                updated[0] = true;
            } catch (Exception e) {
                showError("Invalid country: " + e.getMessage());
            }
        });

        return updated[0] ? countryRepository.findById(country.getId()) : null;
    }

    private Location openAddLocationDialog(Window owner) {
        Dialog<Location> dialog = new Dialog<>();
        dialog.setTitle("Add Location");
        dialog.setHeaderText("Enter location details (name required)");
        applyDialogTheme(dialog, "form-dialog");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType addButtonType = new ButtonType("Add Location", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField cityField = new TextField();
        TextField addressField = new TextField();
        TextField latitudeField = new TextField();
        TextField longitudeField = new TextField();
        TextField imagePathField = new TextField();
        imagePathField.setEditable(false);

        ComboBox<Country> countryCombo = new ComboBox<>();
        Runnable refreshCountries = () -> {
            Country selectedCountry = countryCombo.getValue();
            countryCombo.getItems().setAll(countryRepository.getCountries());
            if (countryCombo.getItems().isEmpty()) {
                countryCombo.getSelectionModel().clearSelection();
                return;
            }
            if (selectedCountry == null || !countryCombo.getItems().contains(selectedCountry)) {
                countryCombo.getSelectionModel().selectFirst();
            }
        };
        refreshCountries.run();
        countryCombo.setConverter(createCountryConverter());
        configureCountryComboForDelete(countryCombo, refreshCountries);
        if (!countryCombo.getItems().isEmpty()) {
            countryCombo.getSelectionModel().selectFirst();
        }

        Button newCountryButton = createAddButton("New...");
        newCountryButton.setOnAction(e -> {
            Country country = openAddCountryDialog(dialog.getDialogPane().getScene().getWindow());
            if (country != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(country);
            }
        });

        Button editCountryButton = createEditButton("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = createDeleteButton("Delete");
        deleteCountryButton.setOnAction(e -> deleteCountryFromUi(countryCombo.getValue(), refreshCountries));

        Button uploadButton = new Button("Upload Image...");
        uploadButton.setOnAction(e -> chooseImagePath(dialog, imagePathField));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Country"), 0, 1);
        grid.add(createResponsiveActionRow(countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 1);
        grid.add(new Label("City"), 0, 2);
        grid.add(cityField, 1, 2);
        grid.add(new Label("Address"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Latitude"), 0, 4);
        grid.add(latitudeField, 1, 4);
        grid.add(new Label("Longitude"), 0, 5);
        grid.add(longitudeField, 1, 5);
        grid.add(new Label("Image"), 0, 6);
        grid.add(new HBox(8, imagePathField, uploadButton), 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != addButtonType) {
                return null;
            }
            try {
                Country country = countryCombo.getValue();
                if (country == null) {
                    throw new IllegalArgumentException("Country is required");
                }
                return locationRepository.addLocation(
                        nameField.getText(),
                        addressField.getText(),
                        cityField.getText(),
                        country.getId(),
                        parseOptionalDouble(latitudeField.getText()),
                        parseOptionalDouble(longitudeField.getText()),
                        imagePathField.getText()
                );
            } catch (Exception ex) {
                showError("Invalid location: " + ex.getMessage());
                return null;
            }
        });

        return dialog.showAndWait().orElse(null);
    }

    private Location openEditLocationDialog(Window owner, Location location) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Location");
        dialog.setHeaderText("Update location details");
        applyDialogTheme(dialog, "form-dialog");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(location.getName());
        TextField cityField = new TextField(location.getCity() == null ? "" : location.getCity());
        TextField addressField = new TextField(location.getAddress() == null ? "" : location.getAddress());
        TextField latitudeField = new TextField(location.getLatitude() == null ? "" : location.getLatitude().toString());
        TextField longitudeField = new TextField(location.getLongitude() == null ? "" : location.getLongitude().toString());
        TextField imagePathField = new TextField(location.getImagePath() == null ? "" : location.getImagePath());
        imagePathField.setEditable(false);

        ComboBox<Country> countryCombo = new ComboBox<>();
        Runnable refreshCountries = () -> {
            Country selectedCountry = countryCombo.getValue();
            countryCombo.getItems().setAll(countryRepository.getCountries());
            if (countryCombo.getItems().isEmpty()) {
                countryCombo.getSelectionModel().clearSelection();
                return;
            }
            if (selectedCountry == null || !countryCombo.getItems().contains(selectedCountry)) {
                countryCombo.getSelectionModel().selectFirst();
            }
        };
        refreshCountries.run();
        countryCombo.setConverter(createCountryConverter());
        configureCountryComboForDelete(countryCombo, refreshCountries);
        if (location.getCountry() != null && countryCombo.getItems().contains(location.getCountry())) {
            countryCombo.getSelectionModel().select(location.getCountry());
        }

        Button newCountryButton = createAddButton("New...");
        newCountryButton.setOnAction(e -> {
            Country created = promptAddCountry();
            if (created != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(created);
            }
        });

        Button editCountryButton = createEditButton("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = createDeleteButton("Delete");
        deleteCountryButton.setOnAction(e -> deleteCountryFromUi(countryCombo.getValue(), refreshCountries));

        Button uploadButton = new Button("Upload Image...");
        uploadButton.setOnAction(e -> chooseImagePath(dialog, imagePathField));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Country"), 0, 1);
        grid.add(createResponsiveActionRow(countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 1);
        grid.add(new Label("City"), 0, 2);
        grid.add(cityField, 1, 2);
        grid.add(new Label("Address"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Latitude"), 0, 4);
        grid.add(latitudeField, 1, 4);
        grid.add(new Label("Longitude"), 0, 5);
        grid.add(longitudeField, 1, 5);
        grid.add(new Label("Image"), 0, 6);
        grid.add(new HBox(8, imagePathField, uploadButton), 1, 6);

        dialog.getDialogPane().setContent(grid);
        final boolean[] updated = new boolean[]{false};
        dialog.showAndWait().ifPresent(result -> {
            if (result != saveButtonType) {
                return;
            }
            try {
                Country selectedCountry = countryCombo.getValue();
                if (selectedCountry == null) {
                    throw new IllegalArgumentException("Country is required");
                }
                locationRepository.updateLocation(
                        location.getId(),
                        nameField.getText(),
                        addressField.getText(),
                        cityField.getText(),
                        selectedCountry.getId(),
                        parseOptionalDouble(latitudeField.getText()),
                        parseOptionalDouble(longitudeField.getText()),
                        imagePathField.getText());
                updated[0] = true;
            } catch (Exception e) {
                showError("Invalid location: " + e.getMessage());
            }
        });

        return updated[0] ? locationRepository.findById(location.getId()) : null;
    }

    private void chooseImagePath(Dialog<?> dialog, TextField targetField) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        Window window = dialog.getDialogPane().getScene() != null ? dialog.getDialogPane().getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            targetField.setText(file.getAbsolutePath());
        }
    }

    private Button createAddButton(String text) {
        Button button = new Button(text);
        applyButtonStyleClass(button, "add-button");
        return button;
    }

    private Button createEditButton(String text) {
        Button button = new Button(text);
        applyButtonStyleClass(button, "edit-button");
        return button;
    }

    private Button createDeleteButton(String text) {
        Button button = new Button(text);
        applyButtonStyleClass(button, "delete-button");
        return button;
    }

    private HBox createResponsiveActionRow(ComboBox<?> combo, Button addButton, Button editButton, Button deleteButton) {
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setPrefWidth(260);
        HBox.setHgrow(combo, Priority.ALWAYS);
        lockButtonWidth(addButton);
        lockButtonWidth(editButton);
        lockButtonWidth(deleteButton);
        return new HBox(8, combo, addButton, editButton, deleteButton);
    }

    private void lockButtonWidth(Button button) {
        button.setMinWidth(84);
    }

    private void applyButtonStyleClass(Button button, String styleClass) {
        if (!button.getStyleClass().contains(styleClass)) {
            button.getStyleClass().add(styleClass);
        }
    }

    private void saveLookupStores() {
        try {
            countryRepository.save();
            locationRepository.save();
            expenseRepository.save();
        } catch (IOException e) {
            showError("Failed to save lookup data: " + e.getMessage());
        }
    }

    public void configureLocationComboForDelete(ComboBox<Location> locationCombo, Runnable onDataChanged) {
        Callback<ListView<Location>, ListCell<Location>> factory = ignored -> createLocationCellWithDelete(onDataChanged);
        locationCombo.setCellFactory(factory);
        locationCombo.setButtonCell(createLocationCellWithDelete(onDataChanged));
    }

    private void configureCountryComboForDelete(ComboBox<Country> countryCombo, Runnable onDataChanged) {
        Callback<ListView<Country>, ListCell<Country>> factory = ignored -> createCountryCellWithDelete(onDataChanged);
        countryCombo.setCellFactory(factory);
        countryCombo.setButtonCell(createCountryCellWithDelete(onDataChanged));
    }

    private ListCell<Country> createCountryCellWithDelete(Runnable onDataChanged) {
        ListCell<Country> cell = new ListCell<>() {
            @Override
            protected void updateItem(Country item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        };
        MenuItem deleteItem = new MenuItem("Delete Country");
        deleteItem.setOnAction(event -> {
            Country country = cell.getItem();
            if (country != null) {
                attemptDeleteCountry(country, onDataChanged);
            }
        });
        ContextMenu contextMenu = new ContextMenu(deleteItem);
        cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> cell.setContextMenu(isEmpty ? null : contextMenu));
        return cell;
    }

    private ListCell<Location> createLocationCellWithDelete(Runnable onDataChanged) {
        ListCell<Location> cell = new ListCell<>() {
            @Override
            protected void updateItem(Location item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        };
        MenuItem deleteItem = new MenuItem("Delete Location");
        deleteItem.setOnAction(event -> {
            Location location = cell.getItem();
            if (location != null) {
                attemptDeleteLocation(location, onDataChanged);
            }
        });
        ContextMenu contextMenu = new ContextMenu(deleteItem);
        cell.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> cell.setContextMenu(isEmpty ? null : contextMenu));
        return cell;
    }

    private boolean attemptDeleteCountry(Country country, Runnable onDataChanged) {
        int countryId = country.getId();
        List<String> tripReferences = findCountryReferences(countryId);
        if (!tripReferences.isEmpty()) {
            showError("Cannot delete country '" + country.getName() + "'.\nReferenced by trips:\n- "
                    + String.join("\n- ", tripReferences));
            return false;
        }

        List<Location> locationsInCountry = new ArrayList<>();
        for (Location location : locationRepository.getLocations()) {
            if (location.getCountry() != null && location.getCountry().getId() == countryId) {
                locationsInCountry.add(location);
            }
        }

        List<String> blockingActivityReferences = new ArrayList<>();
        for (Location location : locationsInCountry) {
            List<String> locationReferences = findLocationReferences(location.getId());
            if (!locationReferences.isEmpty()) {
                blockingActivityReferences.add("Location: " + location.getName());
                for (String reference : locationReferences) {
                    blockingActivityReferences.add("  " + reference);
                }
            }
        }

        if (!blockingActivityReferences.isEmpty()) {
            showError("Cannot delete country '" + country.getName()
                    + "'.\nSome linked locations are still referenced by activities:\n- "
                    + String.join("\n- ", blockingActivityReferences));
            return false;
        }

        try {
            int deletedLocationCount = 0;
            for (Location location : locationsInCountry) {
                locationRepository.deleteLocationById(location.getId());
                deletedLocationCount++;
            }
            countryRepository.deleteCountryById(country.getId());
            saveLookupStores();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            String deleteMessage = "Deleted " + country.getName();
            if (deletedLocationCount > 0) {
                deleteMessage += " and " + deletedLocationCount + " linked location(s).";
            }
            showInfo("Country Deleted", deleteMessage);
            refreshHeaderActivitySummary();
            return true;
        } catch (Exception e) {
            showError("Failed to delete country: " + e.getMessage());
            return false;
        }
    }

    private boolean attemptDeleteLocation(Location location, Runnable onDataChanged) {
        List<String> references = findLocationReferences(location.getId());
        if (!references.isEmpty()) {
            showError("Cannot delete location '" + location.getName() + "'.\nReferenced by:\n- "
                    + String.join("\n- ", references));
            return false;
        }

        try {
            locationRepository.deleteLocationById(location.getId());
            saveLookupStores();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            showInfo("Location Deleted", "Deleted " + location.getName());
            refreshHeaderActivitySummary();
            return true;
        } catch (Exception e) {
            showError("Failed to delete location: " + e.getMessage());
            return false;
        }
    }

    public List<String> findCountryReferences(int countryId) {
        List<String> references = new ArrayList<>();
        for (Trip trip : tripManager.getTrips()) {
            if (trip.getCountry() != null && trip.getCountry().getId() == countryId) {
                references.add("Trip: " + trip.getName());
            }
        }
        return references;
    }

    public List<String> findLocationReferences(int locationId) {
        List<String> references = new ArrayList<>();
        for (Trip trip : tripManager.getTrips()) {
            for (Activity activity : trip.getActivities()) {
                if (activity.getLocation() != null && activity.getLocation().getId() == locationId) {
                    references.add("Activity: " + activity.getName() + " (Trip: " + trip.getName() + ")");
                }
            }
        }
        return references;
    }

    public void refreshHeaderActivitySummary() {
        if (ongoingActivityContainer == null || upcomingActivityContainer == null) {
            return;
        }

        ongoingActivityContainer.getChildren().clear();
        upcomingActivityContainer.getChildren().clear();

        LocalDateTime now = LocalDateTime.now();
        List<ActivitySummaryEntry> activityEntries = new ArrayList<>();
        for (Trip trip : tripManager.getTrips()) {
            for (Activity activity : trip.getActivities()) {
                activityEntries.add(new ActivitySummaryEntry(activity, trip.getName()));
            }
        }

        List<ActivitySummaryEntry> ongoing = activityEntries.stream()
                .filter(entry -> !entry.activity.getStartDateTime().isAfter(now)
                        && entry.activity.getEndDateTime().isAfter(now))
                .sorted(SNAPSHOT_ACTIVITY_COMPARATOR)
                .toList();

        List<ActivitySummaryEntry> upcoming = activityEntries.stream()
                .filter(entry -> entry.activity.getStartDateTime().isAfter(now))
                .sorted(SNAPSHOT_ACTIVITY_COMPARATOR)
                .toList();

        if (ongoing.isEmpty()) {
            ongoingActivityContainer.getChildren().add(createSnapshotEmptyCard("No ongoing activities"));
        } else {
            for (ActivitySummaryEntry entry : ongoing) {
                ongoingActivityContainer.getChildren().add(createSnapshotCard(entry, true));
            }
        }

        if (upcoming.isEmpty()) {
            upcomingActivityContainer.getChildren().add(createSnapshotEmptyCard("No upcoming activities"));
        } else {
            for (ActivitySummaryEntry entry : upcoming) {
                upcomingActivityContainer.getChildren().add(createSnapshotCard(entry, false));
            }
        }
    }

    private void refreshTripList() {
        List<Trip> sortedTrips = tripManager.getTrips().stream()
                .sorted(TRIP_DISPLAY_COMPARATOR)
                .toList();
        tripObservableList.setAll(sortedTrips);
    }

    private VBox createSnapshotCard(ActivitySummaryEntry entry, boolean isOngoing) {
        VBox card = new VBox(4);
        card.getStyleClass().add("snapshot-item");

        Label activityName = new Label(entry.activity.getName());
        activityName.getStyleClass().add("snapshot-name");
        activityName.setWrapText(true);

        String timeText = isOngoing
                ? "Until " + entry.activity.getEndDateTime().format(DATE_TIME_FORMAT)
                : entry.activity.getStartDateTime().format(DATE_TIME_FORMAT);
        Label timeLabel = new Label(timeText);
        timeLabel.getStyleClass().add("snapshot-meta");

        HBox tagRow = new HBox(6);
        Label tripTag = createSnapshotTag("Trip: " + entry.tripName);
        String typeText = entry.activity.getTypes().isEmpty() ? "Type: OTHER"
                : "Type: " + entry.activity.getTypes().get(0).name();
        Label typeTag = createSnapshotTag(typeText);
        tagRow.getChildren().addAll(tripTag, typeTag);

        card.getChildren().addAll(activityName, timeLabel, tagRow);
        return card;
    }

    private Label createSnapshotTag(String text) {
        Label tag = new Label(text);
        tag.getStyleClass().add("snapshot-tag");
        return tag;
    }

    private VBox createSnapshotEmptyCard(String text) {
        VBox card = new VBox();
        card.getStyleClass().addAll("snapshot-item", "snapshot-empty");
        Label label = new Label(text);
        label.getStyleClass().add("snapshot-meta");
        card.getChildren().add(label);
        return card;
    }

    private static class ActivitySummaryEntry {
        private final Activity activity;
        private final String tripName;

        private ActivitySummaryEntry(Activity activity, String tripName) {
            this.activity = activity;
            this.tripName = tripName;
        }
    }

    private void showHomeGuide() {
        showGuideDialog("Home Page Guide", "Top-level controls",
                List.of(
                "Use Add Trip to create a new trip.",
                "Select a trip and use Edit Trip to update details.",
                "Select a trip and use Delete Trip to remove it.",
                "Double-click a trip card to open its itinerary and expenses.",
                "Inside add/edit forms, use New, Edit, and Delete beside selectors to manage countries and locations."
                ));
    }

    private void showGuideDialog(String title, String header, List<String> pageNotes) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        applyDialogTheme(dialog, "guide-dialog");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.getStyleClass().add("guide-content");

        VBox quickStartSection = createGuideSection("Quick Start", List.of(
            "Open this guide any time using the ? button on the current page.",
            "Use Add to create items, Edit to update items, and Delete to remove items.",
            "Use Back buttons to return to the previous page.",
            "Use selector buttons in forms to manage countries and locations without leaving the dialog."
        ));

        VBox pageSection = createGuideSection("This Page", pageNotes);
        content.getChildren().addAll(quickStartSection, pageSection);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private VBox createGuideSection(String title, List<String> notes) {
        VBox section = new VBox(6);
        section.getStyleClass().add("guide-section");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("guide-section-title");
        section.getChildren().add(titleLabel);

        for (String note : notes) {
            HBox row = new HBox(8);
            row.getStyleClass().add("guide-note-row");
            Label bullet = new Label("•");
            bullet.getStyleClass().add("guide-bullet");
            Label text = new Label(note);
            text.setWrapText(true);
            text.getStyleClass().add("guide-text");
            HBox.setHgrow(text, Priority.ALWAYS);
            row.getChildren().addAll(bullet, text);
            section.getChildren().add(row);
        }
        return section;
    }

    private void applyDialogTheme(Dialog<?> dialog, String styleClass) {
        String stylesheet = getClass().getResource("/view/theme.css").toExternalForm();
        if (!dialog.getDialogPane().getStylesheets().contains(stylesheet)) {
            dialog.getDialogPane().getStylesheets().add(stylesheet);
        }
        if (styleClass != null && !styleClass.isBlank() && !dialog.getDialogPane().getStyleClass().contains(styleClass)) {
            dialog.getDialogPane().getStyleClass().add(styleClass);
        }

        applyDialogActionStyles(dialog);
        dialog.getDialogPane().getButtonTypes().addListener((ListChangeListener<ButtonType>) change ->
                applyDialogActionStyles(dialog));
    }

    private void applyDialogActionStyles(Dialog<?> dialog) {
        for (ButtonType buttonType : dialog.getDialogPane().getButtonTypes()) {
            if (!buttonType.getButtonData().isCancelButton()) {
                continue;
            }
            Node node = dialog.getDialogPane().lookupButton(buttonType);
            if (node instanceof Button button) {
                if (dialog.getDialogPane().getStyleClass().contains("guide-dialog")) {
                    applyButtonStyleClass(button, "secondary-button");
                } else {
                    applyButtonStyleClass(button, "delete-button");
                }
            }
        }
    }

    private Image resolveImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            String normalizedPath = imageAssetStore.normalizeImagePath(imagePath);
            if (normalizedPath == null || normalizedPath.isBlank()) {
                return null;
            }

            if (normalizedPath.startsWith("/images/")) {
                InputStream stream = getClass().getResourceAsStream(normalizedPath);
                if (stream != null) {
                    return new Image(stream);
                }
                return null;
            }

            File imageFile = new File(normalizedPath);
            if (!imageFile.exists()) {
                return null;
            }
            return new Image(new FileInputStream(imageFile));
        } catch (Exception e) {
            return null;
        }
    }

    private StringConverter<Country> createCountryConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Country country) {
                return country == null ? "" : country.toString();
            }

            @Override
            public Country fromString(String string) {
                return null;
            }
        };
    }

    private Double parseOptionalDouble(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return Double.parseDouble(text.trim());
    }

    private LocalTime parseTimeOrDefault(String text, LocalTime fallback) {
        try {
            return LocalTime.parse(text);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String formatDateTimeRange(LocalDateTime start, LocalDateTime end) {
        return formatDateTime(start) + " to " + formatDateTime(end);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : "?";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
