package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores user-selected image assets into a local data directory.
 */
public class ImageAssetStore {

    private static final Path IMAGE_DIR = Paths.get("data", "images");
    private static final String DATA_IMAGE_PREFIX = "data/images/";
    private static final Map<String, String> LEGACY_IMAGE_MAP = Map.of(
            "/images/city.jpg", "/images/singapore.jpg",
            "/images/nature.jpg", "/images/japan.jpg",
            "/images/market.jpg", "/images/china.jpg",
            "/images/food.jpg", "/images/thailand.jpg",
            "/images/culture.jpg", "/images/malaysia.jpg",
            "/images/beach.jpg", "/images/philippines.jpg",
            "/images/philipines.jpg", "/images/philippines.jpg"
    );

    public String importImage(String sourcePath, String keyPrefix) {
        return importImage(sourcePath, keyPrefix, null);
    }

    public String importImage(String sourcePath, String keyPrefix, String semanticName) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }

        String normalizedSource = normalizeImagePath(sourcePath);
        if (normalizedSource != null
                && (normalizedSource.startsWith("/images/") || normalizedSource.startsWith(DATA_IMAGE_PREFIX))) {
            return normalizedSource;
        }

        Path source = Paths.get(sourcePath.trim());
        if (!Files.exists(source)) {
            return null;
        }

        String extension = getExtension(source.getFileName().toString());
        String safePrefix = keyPrefix == null || keyPrefix.isBlank() ? "asset" : keyPrefix.toLowerCase(Locale.ROOT);
        String baseName = semanticName == null || semanticName.isBlank()
                ? stripExtension(source.getFileName().toString())
                : semanticName;
        String slug = toSlug(baseName);
        String fileName = safePrefix + "-" + slug + extension;

        try {
            Files.createDirectories(IMAGE_DIR);
            Path target = IMAGE_DIR.resolve(fileName);
            if (Files.exists(target)) {
                String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
                target = IMAGE_DIR.resolve(safePrefix + "-" + slug + "-" + uniqueSuffix + extension);
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return DATA_IMAGE_PREFIX + target.getFileName();
        } catch (IOException e) {
            return null;
        }
    }

    public String normalizeImagePath(String imagePath) {
        if (imagePath == null) {
            return null;
        }

        String normalized = imagePath.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replace('\\', '/');

        String mappedLegacy = LEGACY_IMAGE_MAP.get(normalized.toLowerCase(Locale.ROOT));
        if (mappedLegacy != null) {
            return mappedLegacy;
        }

        if (normalized.startsWith("/images/")) {
            return normalized;
        }
        if (normalized.startsWith("images/")) {
            return "/" + normalized;
        }
        if (normalized.startsWith(DATA_IMAGE_PREFIX)) {
            return normalized;
        }

        int relativeIndex = normalized.toLowerCase(Locale.ROOT).indexOf(DATA_IMAGE_PREFIX);
        if (relativeIndex >= 0) {
            return normalized.substring(relativeIndex);
        }

        return normalized;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return ".png";
        }
        return fileName.substring(dotIndex);
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private String toSlug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "image" : normalized;
    }
}
