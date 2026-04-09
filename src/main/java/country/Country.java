package country;

import utilities.BaseEntity;

import java.util.Objects;

/**
 * A generic country-level domain object used by trips and locations.
 */
public class Country extends BaseEntity {

    private String continent;
    private String imagePath;

    public Country(int id, String name) {
        this(id, name, null, null);
    }

    public Country(int id, String name, String continent, String imagePath) {
        super(id, name);
        this.continent = normalizeOptional(continent);
        this.imagePath = normalizeOptional(imagePath);
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = normalizeOptional(continent);
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = normalizeOptional(imagePath);
    }

    @Override
    public String toString() {
        if (continent == null) {
            return getName();
        }
        return getName() + " (" + continent + ")";
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
