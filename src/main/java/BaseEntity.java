import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Base class for all entities with shared identity and descriptive fields.
 */
public abstract class BaseEntity {

    private int id;

    private String name;

    private String description;

    private BufferedImage image;

    protected BaseEntity() {
        this(0, "");
    }

    protected BaseEntity(int id, String name) {
        setId(id);
        setName(name);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

}
