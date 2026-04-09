package utilities;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Base class for all entities with shared identity and descriptive fields.
 */
public abstract class BaseEntity {

    private int id;

    private String name;

    private String description;

    private int priority;

    // BufferedImage should remain runtime-only and never be serialized to JSON.
    private transient BufferedImage image;

    protected BaseEntity() {
        this(0, "Unnamed");
    }

    protected BaseEntity(int id, String name) {
        setId(id);
        setName(name);
        setPriority(0);
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
        String trimmed = Objects.requireNonNull(name, "name").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = trimmed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < 0) {
            throw new IllegalArgumentException("priority must be non-negative");
        }
        this.priority = priority;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

}
