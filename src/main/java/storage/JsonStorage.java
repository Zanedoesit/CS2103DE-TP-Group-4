package storage;

import activity.Activity;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import expense.Expense;
import location.Location;
import trip.Trip;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles saving and loading Trip data to/from a JSON file.
 *
 * HOW IT WORKS (for beginners):
 * =============================
 * 1. We use a library called "Gson" (by Google) that can convert
 *    Java objects into JSON text and back again.
 *
 * 2. When we SAVE: Gson looks at all the fields in your Trip objects
 *    (name, startDateTime, activities, expenses, etc.) and writes them
 *    out as a structured JSON text file.
 *
 * 3. When we LOAD: Gson reads that JSON text file and recreates the
 *    Java objects (Trip, Activity, Expense, Location) from the data.
 *
 * 4. We need some special configuration because:
 *    - LocalDateTime is not a simple type (Gson needs help with it)
 *    - BufferedImage (for images) can't be stored as JSON text
 *    - Trip and Activity constructors have validation rules
 */
public class JsonStorage {

    /** The folder where we save the JSON file */
    private static final String DATA_DIRECTORY = "data";

    /** The filename for our saved trips */
    private static final String DATA_FILE = "trips.json";

    /** The Gson instance configured with our custom settings */
    private final Gson gson;

    /** The full path to the data file (e.g., "data/trips.json") */
    private final Path dataFilePath;

    /**
     * Creates a new JsonStorage with default settings.
     * The data file will be at "data/trips.json" relative to where the app runs.
     */
    public JsonStorage() {
        this(Paths.get(DATA_DIRECTORY, DATA_FILE));
    }

    /**
     * Creates a new JsonStorage that saves/loads from a specific file path.
     * This constructor is useful for testing with a different file location.
     */
    public JsonStorage(Path dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.gson = createGson();
    }

    /**
     * Configures Gson with all the custom settings we need.
     *
     * Think of this as "teaching" Gson how to handle our specific classes.
     * Out of the box, Gson knows how to handle simple types like String,
     * int, float, etc. But it needs help with:
     *   - LocalDateTime (our custom adapter tells it to use ISO format strings)
     *   - BufferedImage (we tell it to skip these entirely)
     *   - Trip and Activity (we tell it how to reconstruct them properly)
     */
    private Gson createGson() {
        return new GsonBuilder()
                // "Pretty printing" means the JSON file will have nice indentation
                // and line breaks, making it human-readable (great for debugging!)
                .setPrettyPrinting()

                // Register our custom LocalDateTime adapter so Gson knows
                // how to convert dates like 2026-04-03T09:00:00
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())

                // Register custom deserializers for Trip and Activity.
                // These are needed because Trip and Activity constructors
                // validate their inputs (e.g., start must be before end).
                // Gson's default approach of setting fields directly would
                // bypass these checks, so we manually call the constructors.
                .registerTypeAdapter(Trip.class, new TripDeserializer())
                .registerTypeAdapter(Activity.class, new ActivityDeserializer())

                // Tell Gson to skip any BufferedImage fields.
                // Images are binary data and don't belong in a text-based JSON file.
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes field) {
                        return field.getDeclaredType() == BufferedImage.class;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return clazz == BufferedImage.class;
                    }
                })

                .create();
    }

    // ========================================================================
    // PUBLIC METHODS - These are what the rest of the app calls
    // ========================================================================

    /**
     * Saves a list of trips to the JSON file.
     *
     * What happens step by step:
     * 1. Create the "data/" folder if it doesn't exist yet
     * 2. Convert all Trip objects into a JSON string using Gson
     * 3. Write that string to "data/trips.json"
     *
     * @param trips the list of trips to save
     * @throws IOException if writing to the file fails
     */
    public void save(List<Trip> trips) throws IOException {
        // Ensure the data directory exists
        Path directory = dataFilePath.getParent();
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        // Write the JSON to the file
        // try-with-resources automatically closes the writer when done
        try (Writer writer = Files.newBufferedWriter(dataFilePath)) {
            gson.toJson(trips, writer);
        }
    }

    /**
     * Loads trips from the JSON file.
     *
     * What happens step by step:
     * 1. Check if "data/trips.json" exists
     * 2. If not, return an empty list (first time running the app)
     * 3. If yes, read the file and convert JSON back into Trip objects
     *
     * @return the list of trips loaded from the file, or empty list if no file
     * @throws IOException if reading the file fails
     */
    public List<Trip> load() throws IOException {
        if (!Files.exists(dataFilePath)) {
            // No saved data yet — this is fine, just return empty
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(dataFilePath)) {
            // TypeToken tells Gson we want a List<Trip>, not just a single Trip.
            // This is needed because of Java's "type erasure" — at runtime,
            // Java forgets the <Trip> part, so we use TypeToken to preserve it.
            Type listType = new TypeToken<List<Trip>>() {}.getType();
            List<Trip> trips = gson.fromJson(reader, listType);
            return trips != null ? trips : new ArrayList<>();
        } catch (JsonParseException e) {
            // If the file is corrupted or has invalid JSON,
            // print a warning and return empty rather than crashing
            System.err.println("Warning: Could not parse saved data: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Returns the path where data is being stored.
     * Useful for displaying to the user or for debugging.
     */
    public Path getDataFilePath() {
        return dataFilePath;
    }

    // ========================================================================
    // CUSTOM DESERIALIZERS
    // ========================================================================
    // These teach Gson how to reconstruct Trip and Activity objects
    // from JSON. We need these because the constructors have validation
    // (e.g., startDateTime must be before endDateTime), and Gson's default
    // approach would bypass that validation.

    /**
     * Custom deserializer for Trip objects.
     *
     * When Gson reads a Trip from JSON, instead of trying to magically
     * create one, it calls this code which properly uses the Trip constructor.
     */
    private static class TripDeserializer implements JsonDeserializer<Trip> {
        @Override
        public Trip deserialize(JsonElement json, Type typeOfT,
                                JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            // Extract the basic fields from the JSON object
            int id = obj.get("id").getAsInt();
            String name = obj.get("name").getAsString();

            // Parse dates using our LocalDateTime adapter
            LocalDateTime start = context.deserialize(obj.get("startDateTime"), LocalDateTime.class);
            LocalDateTime end = context.deserialize(obj.get("endDateTime"), LocalDateTime.class);

            // Parse location (may be null if none was set)
            Location location = null;
            if (obj.has("location") && !obj.get("location").isJsonNull()) {
                location = context.deserialize(obj.get("location"), Location.class);
            }

            // Create the Trip using its proper constructor (with validation)
            Trip trip = new Trip(id, name, start, end, location);

            // Set optional description
            if (obj.has("description") && !obj.get("description").isJsonNull()) {
                trip.setDescription(obj.get("description").getAsString());
            }

            // Restore activities
            if (obj.has("activities")) {
                JsonArray activitiesArray = obj.getAsJsonArray("activities");
                for (JsonElement actElement : activitiesArray) {
                    Activity activity = context.deserialize(actElement, Activity.class);
                    try {
                        trip.addActivity(activity);
                    } catch (Exception e) {
                        System.err.println("Warning: Skipping activity due to: " + e.getMessage());
                    }
                }
            }

            // Restore trip-level expenses
            if (obj.has("expenses")) {
                JsonArray expensesArray = obj.getAsJsonArray("expenses");
                for (JsonElement expElement : expensesArray) {
                    Expense expense = context.deserialize(expElement, Expense.class);
                    trip.addExpense(expense);
                }
            }

            return trip;
        }
    }

    /**
     * Custom deserializer for Activity objects.
     * Same idea as TripDeserializer — we need to call the constructor properly.
     */
    private static class ActivityDeserializer implements JsonDeserializer<Activity> {
        @Override
        public Activity deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            int id = obj.get("id").getAsInt();
            String name = obj.get("name").getAsString();
            LocalDateTime start = context.deserialize(obj.get("startDateTime"), LocalDateTime.class);
            LocalDateTime end = context.deserialize(obj.get("endDateTime"), LocalDateTime.class);

            Location location = null;
            if (obj.has("location") && !obj.get("location").isJsonNull()) {
                location = context.deserialize(obj.get("location"), Location.class);
            }

            Activity activity = new Activity(id, name, start, end, location);

            if (obj.has("description") && !obj.get("description").isJsonNull()) {
                activity.setDescription(obj.get("description").getAsString());
            }

            // Restore activity types (SIGHTSEEING, ADVENTURE, etc.)
            if (obj.has("types")) {
                JsonArray typesArray = obj.getAsJsonArray("types");
                for (JsonElement typeElement : typesArray) {
                    activity.addType(Activity.Type.valueOf(typeElement.getAsString()));
                }
            }

            // Restore activity-level expenses
            if (obj.has("expenses")) {
                JsonArray expensesArray = obj.getAsJsonArray("expenses");
                for (JsonElement expElement : expensesArray) {
                    Expense expense = context.deserialize(expElement, Expense.class);
                    activity.addExpense(expense);
                }
            }

            return activity;
        }
    }
}