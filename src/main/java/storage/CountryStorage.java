package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import country.Country;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated JSON storage for countries.
 */
public class CountryStorage {

    private static final String DATA_DIRECTORY = "data";
    private static final String DATA_FILE = "countries.json";

    private final Gson gson;
    private final Path dataFilePath;

    public CountryStorage() {
        this(Paths.get(DATA_DIRECTORY, DATA_FILE));
    }

    public CountryStorage(Path dataFilePath) {
        this.dataFilePath = dataFilePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void save(List<Country> countries) throws IOException {
        Path directory = dataFilePath.getParent();
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        try (Writer writer = Files.newBufferedWriter(dataFilePath)) {
            gson.toJson(countries, writer);
        }
    }

    public List<Country> load() throws IOException {
        if (!Files.exists(dataFilePath)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(dataFilePath)) {
            Type listType = new TypeToken<List<Country>>() {}.getType();
            List<Country> countries = gson.fromJson(reader, listType);
            return countries != null ? countries : new ArrayList<>();
        } catch (JsonParseException e) {
            // Recover from partially written/corrupted JSON by resetting to defaults upstream.
            return new ArrayList<>();
        }
    }
}
