package country;

import storage.CountryStorage;
import storage.ImageAssetStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Storage-backed repository for country business logic.
 */
public class CountryRepository {

    private static final Set<Integer> USED_COUNTRY_IDS = new HashSet<>();
    private static final Set<String> USED_COUNTRY_NAMES = new HashSet<>();

    private final List<Country> countries = new ArrayList<>();
    private final Map<Integer, Country> countriesById = new HashMap<>();
    private final CountryStorage storage;
    private final ImageAssetStore imageAssetStore;
    private int nextId = 1;

    public CountryRepository() {
        this(new CountryStorage(), new ImageAssetStore());
    }

    public CountryRepository(CountryStorage storage, ImageAssetStore imageAssetStore) {
        this.storage = storage;
        this.imageAssetStore = imageAssetStore;
    }

    public void load() throws IOException {
        countries.clear();
        countriesById.clear();
        USED_COUNTRY_IDS.clear();
        USED_COUNTRY_NAMES.clear();
        countries.addAll(storage.load());
        nextId = 1;
        boolean hasImagePathUpdates = false;
        for (Country country : countries) {
            registerCountryIdentity(country);
            nextId = Math.max(nextId, country.getId() + 1);
            String normalizedImagePath = imageAssetStore.normalizeImagePath(country.getImagePath());
            if (!Objects.equals(country.getImagePath(), normalizedImagePath)) {
                country.setImagePath(normalizedImagePath);
                hasImagePathUpdates = true;
            }
            countriesById.put(country.getId(), country);
        }
        if (countries.isEmpty()) {
            seedDefaults();
            save();
        } else if (hasImagePathUpdates) {
            save();
        }
    }

    public void save() throws IOException {
        storage.save(countries);
    }

    public List<Country> getCountries() {
        return Collections.unmodifiableList(countries);
    }

    public Country addCountry(String name, String continent, String imageSourcePath) {
        String normalizedName = normalizeRequired(name, "country name");
        int candidateId = nextId;
        ensureUniqueCountryName(normalizedName);
        ensureUniqueCountryId(candidateId);

        String storedImagePath = imageAssetStore.importImage(imageSourcePath, "country", normalizedName);
        Country country = new Country(candidateId, normalizedName, normalizeOptional(continent), storedImagePath);
        registerCountryIdentity(country);
        nextId++;
        countries.add(country);
        countriesById.put(country.getId(), country);
        return country;
    }

    public Country updateCountry(int countryId, String name, String continent, String imageSourcePath) {
        Country country = countriesById.get(countryId);
        if (country == null) {
            throw new IllegalArgumentException("Country not found: id=" + countryId);
        }

        String normalizedName = normalizeRequired(name, "country name");
        String oldNameKey = normalizeRequired(country.getName(), "country name").toLowerCase();
        String newNameKey = normalizedName.toLowerCase();
        if (!oldNameKey.equals(newNameKey) && USED_COUNTRY_NAMES.contains(newNameKey)) {
            throw new IllegalArgumentException("Country already exists: " + normalizedName);
        }

        if (!oldNameKey.equals(newNameKey)) {
            USED_COUNTRY_NAMES.remove(oldNameKey);
            USED_COUNTRY_NAMES.add(newNameKey);
        }

        country.setName(normalizedName);
        country.setContinent(normalizeOptional(continent));

        if (imageSourcePath != null && !imageSourcePath.isBlank()) {
            String storedImagePath = imageAssetStore.importImage(imageSourcePath, "country", normalizedName);
            if (storedImagePath == null) {
                storedImagePath = imageAssetStore.normalizeImagePath(imageSourcePath);
            }
            country.setImagePath(storedImagePath);
        }

        return country;
    }

    public Country findById(int countryId) {
        return countriesById.get(countryId);
    }

    public Country findByName(String name) {
        String normalized = normalizeOptional(name);
        if (normalized == null) {
            return null;
        }
        for (Country country : countries) {
            if (country.getName().equalsIgnoreCase(normalized)) {
                return country;
            }
        }
        return null;
    }

    public void deleteCountryById(int countryId) {
        Country country = countriesById.get(countryId);
        if (country == null) {
            throw new IllegalArgumentException("Country not found: id=" + countryId);
        }
        countries.remove(country);
        countriesById.remove(countryId);
        USED_COUNTRY_IDS.remove(country.getId());
        USED_COUNTRY_NAMES.remove(country.getName().trim().toLowerCase());
    }

    private void ensureUniqueCountryName(String name) {
        String normalizedNameKey = name.toLowerCase();
        if (USED_COUNTRY_NAMES.contains(normalizedNameKey)) {
            throw new IllegalArgumentException("Country already exists: " + name);
        }
    }

    private void ensureUniqueCountryId(int id) {
        if (USED_COUNTRY_IDS.contains(id)) {
            throw new IllegalArgumentException("Duplicate country id detected: " + id);
        }
    }

    private void registerCountryIdentity(Country country) {
        String nameKey = normalizeRequired(country.getName(), "country name").toLowerCase();
        if (!USED_COUNTRY_IDS.add(country.getId())) {
            throw new IllegalStateException("Duplicate country id detected: " + country.getId());
        }
        if (!USED_COUNTRY_NAMES.add(nameKey)) {
            throw new IllegalStateException("Duplicate country name detected: " + country.getName());
        }
    }

    private void seedDefaults() {
        countries.add(new Country(nextId++, "Singapore", "Asia", "/images/singapore.jpg"));
        countries.add(new Country(nextId++, "Japan", "Asia", "/images/japan.jpg"));
        countries.add(new Country(nextId++, "United Kingdom", "Europe", "/images/china.jpg"));
        for (Country country : countries) {
            registerCountryIdentity(country);
            countriesById.put(country.getId(), country);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
