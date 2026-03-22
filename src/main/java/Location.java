import java.util.Objects;

/**
 * A physical location with geographic coordinates.
 */
public class Location extends BaseEntity {

    private String address;

    private String city;

    private String country;

    private double latitude;

    private double longitude;

    public Location(int id, String name) {
        super(id, name);
    }

    public Location(int id, String name, String address, String city, String country, double latitude, double longitude) {
        super(id, name);
        this.address = address;
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double distanceTo(Location other) {
        Objects.requireNonNull(other, "other");
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

}
