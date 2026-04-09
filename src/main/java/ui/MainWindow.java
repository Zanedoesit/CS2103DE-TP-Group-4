package ui;

import activity.Activity;
import country.Country;
import country.CountryRepository;
import expense.ExpenseRepository;
import exceptions.TimeIntervalConflictException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    private Label ongoingActivityLabel;
    @FXML
    private Label upcomingActivityLabel;

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
        tripObservableList.setAll(tripManager.getTrips());
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
            showError("Failed to load trip page: " + e.getMessage());
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
                "Use this page to review timeline, activities, and expenses.\n"
                + "- Top ? button: open this guide.\n"
                + "- Use the New... buttons inside forms to add countries/locations.\n"
                        + "- Timeline shows whole activities only per day (no partial clipping).\n"
                        + "- Filter activities by type from the dropdown.");
    }

    public void showActivityGuide() {
        showGuideDialog("Activity Page Guide", "Activity expense controls",
                "Use this page to manage expenses linked to one activity.\n"
                        + "- Top ? button: open this guide.\n"
                + "- Use the New... buttons inside forms to add countries/locations.\n"
                        + "- Add Expense to attach costs to the current activity.");
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

        Button newCountryButton = new Button("New...");
        newCountryButton.setOnAction(e -> {
            Country country = openAddCountryDialog(dialog.getDialogPane().getScene().getWindow());
            if (country != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(country);
            }
        });

        Button editCountryButton = new Button("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = new Button("Delete");
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
        grid.add(new HBox(8, countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 5);

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
                tripObservableList.setAll(tripManager.getTrips());
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
        try {
            tripManager.deleteTripById(selected.getId());
            tripManager.saveToFile();
            tripObservableList.setAll(tripManager.getTrips());
            refreshHeaderActivitySummary();
        } catch (Exception e) {
            showError("Failed to delete trip: " + e.getMessage());
        }
    }

    private void handleEditTrip() {
        Trip selected = tripListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a trip to edit.");
            return;
        }

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

        Button newCountryButton = new Button("New...");
        newCountryButton.setOnAction(e -> {
            Country created = promptAddCountry();
            if (created != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(created);
            }
        });

        Button editCountryButton = new Button("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = new Button("Delete");
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
        grid.add(new HBox(8, countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 5);

        dialog.getDialogPane().setContent(grid);
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
                tripObservableList.setAll(tripManager.getTrips());
                refreshHeaderActivitySummary();
            } catch (Exception e) {
                showError("Failed to edit trip: " + e.getMessage());
            }
        });
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

        Button newCountryButton = new Button("New...");
        newCountryButton.setOnAction(e -> {
            Country country = openAddCountryDialog(dialog.getDialogPane().getScene().getWindow());
            if (country != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(country);
            }
        });

        Button editCountryButton = new Button("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = new Button("Delete");
        deleteCountryButton.setOnAction(e -> deleteCountryFromUi(countryCombo.getValue(), refreshCountries));

        Button uploadButton = new Button("Upload Image...");
        uploadButton.setOnAction(e -> chooseImagePath(dialog, imagePathField));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Country"), 0, 1);
        grid.add(new HBox(8, countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 1);
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

        Button newCountryButton = new Button("New...");
        newCountryButton.setOnAction(e -> {
            Country created = promptAddCountry();
            if (created != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(created);
            }
        });

        Button editCountryButton = new Button("Edit...");
        editCountryButton.setOnAction(e -> {
            Country edited = promptEditCountry(countryCombo.getValue());
            if (edited != null) {
                refreshCountries.run();
                countryCombo.getSelectionModel().select(edited);
            }
        });

        Button deleteCountryButton = new Button("Delete");
        deleteCountryButton.setOnAction(e -> deleteCountryFromUi(countryCombo.getValue(), refreshCountries));

        Button uploadButton = new Button("Upload Image...");
        uploadButton.setOnAction(e -> chooseImagePath(dialog, imagePathField));

        grid.add(new Label("Name*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Country"), 0, 1);
        grid.add(new HBox(8, countryCombo, newCountryButton, editCountryButton, deleteCountryButton), 1, 1);
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
        if (ongoingActivityLabel == null || upcomingActivityLabel == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Activity> activities = new ArrayList<>();
        for (Trip trip : tripManager.getTrips()) {
            activities.addAll(trip.getActivities());
        }

        List<Activity> ongoing = activities.stream()
                .filter(activity -> !activity.getStartDateTime().isAfter(now)
                        && activity.getEndDateTime().isAfter(now))
                .sorted(Comparator.comparing(Activity::getEndDateTime))
                .toList();

        Activity nextUpcoming = activities.stream()
                .filter(activity -> activity.getStartDateTime().isAfter(now))
                .min(Comparator.comparing(Activity::getStartDateTime))
                .orElse(null);

        if (ongoing.isEmpty()) {
            ongoingActivityLabel.setText("Ongoing: none");
        } else {
            Activity first = ongoing.get(0);
            ongoingActivityLabel.setText("Ongoing: " + first.getName() + " (until "
                    + first.getEndDateTime().format(DATE_TIME_FORMAT) + ")");
        }

        if (nextUpcoming == null) {
            upcomingActivityLabel.setText("Upcoming: none");
        } else {
            upcomingActivityLabel.setText("Upcoming: " + nextUpcoming.getName() + " ("
                    + nextUpcoming.getStartDateTime().format(DATE_TIME_FORMAT) + ")");
        }
    }

    private void showHomeGuide() {
        showGuideDialog("Home Page Guide", "Top-level controls",
                "Use this page to manage your trips.\n"
                + "- Top ? button: show this page-specific guide.\n"
            + "- Use Delete buttons near country/location selectors (or right-click dropdown entries).\n"
                        + "- Add Trip: create a trip linked to a country.\n"
                        + "- Double-click a trip to open itinerary details.");
    }

    private void showGuideDialog(String title, String header, String body) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        applyDialogTheme(dialog, "guide-dialog");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(8);
        content.getStyleClass().add("guide-content");

        Label bodyLabel = new Label(body);
        bodyLabel.setWrapText(true);
        bodyLabel.getStyleClass().add("guide-text");

        content.getChildren().add(bodyLabel);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void applyDialogTheme(Dialog<?> dialog, String styleClass) {
        String stylesheet = getClass().getResource("/view/theme.css").toExternalForm();
        if (!dialog.getDialogPane().getStylesheets().contains(stylesheet)) {
            dialog.getDialogPane().getStylesheets().add(stylesheet);
        }
        if (styleClass != null && !styleClass.isBlank() && !dialog.getDialogPane().getStyleClass().contains(styleClass)) {
            dialog.getDialogPane().getStyleClass().add(styleClass);
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
