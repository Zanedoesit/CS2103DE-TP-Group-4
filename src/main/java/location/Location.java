package location;
import country.Country;
import java.util.Objects;
import java.util.StringJoiner;

import utilities.BaseEntity;

/**
 * A physical location with geographic coordinates.
 */
public class Location extends BaseEntity {

    private String address;

    private String city;

    private Country country;

    private Double latitude;

    private Double longitude;

    private String imagePath;

    public Location(int id, String name) {
        super(id, name);
    }

    public Location(int id, String name, String address, String city, Country country, Double latitude, Double longitude) {
        this(id, name, address, city, country, latitude, longitude, null);
    }

    public Location(int id, String name, String address, String city, Country country,
                    Double latitude, Double longitude, String imagePath) {
        super(id, name);
        this.address = address;
        this.city = city;
        setCountry(country);
        this.latitude = latitude;
        this.longitude = longitude;
        this.imagePath = imagePath;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = Objects.requireNonNull(country, "country");
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double distanceTo(Location other) {
        Objects.requireNonNull(other, "other");
        if (this.latitude == null || this.longitude == null
                || other.latitude == null || other.longitude == null) {
            throw new IllegalStateException("Both locations must have latitude and longitude for distance calculation");
        }
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double startLatRad = Math.toRadians(this.latitude);
        double otherLatRad = Math.toRadians(other.latitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(startLatRad) * Math.cos(otherLatRad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    @Override
    public String toString() {
        StringJoiner locationBits = new StringJoiner(", ");
        if (city != null && !city.isBlank()) {
            locationBits.add(city);
        }
        if (country != null && country.getName() != null && !country.getName().isBlank()) {
            locationBits.add(country.getName());
        }

        String primary = (getName() != null && !getName().isBlank()) ? getName() : "Location";
        String region = locationBits.length() > 0 ? " (" + locationBits + ")" : "";
        String addressPart = (address != null && !address.isBlank()) ? " | " + address : "";
        return primary + region + addressPart;
    }

}
