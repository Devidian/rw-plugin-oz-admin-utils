package de.omegazirkel.risingworld.adminutils.render;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;

/** Stores uploaded client world renders for the experimental satellite layer. */
public final class SatelliteImageStore {
    public static final int PAGE_SIZE = 100;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private final Connection connection;
    private final Path root;

    public record Change(int x, int z, int res, long updatedAtMs, long revision) {}
    public record Changes(int schemaVersion, boolean full, long nextChange, boolean partial,
            Integer nextOffset, List<Change> chunks) {}
    private record StoredImage(int res, String hash) {}

    public SatelliteImageStore(Connection connection, Path pluginPath) throws SQLException {
        this.connection = connection;
        this.root = pluginPath.resolve("satellite").resolve("source");
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS satellite_chunks_v1 (
                        chunk_x INTEGER NOT NULL, chunk_z INTEGER NOT NULL,
                        resolution INTEGER NOT NULL, updated_at_ms INTEGER NOT NULL,
                        revision INTEGER NOT NULL, content_hash TEXT,
                        PRIMARY KEY (chunk_x, chunk_z)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_satellite_chunks_revision ON satellite_chunks_v1(revision)");
        }
        boolean hasHash = false;
        try (var statement = connection.createStatement();
                var result = statement.executeQuery("PRAGMA table_info(satellite_chunks_v1)")) {
            while (result.next()) if ("content_hash".equals(result.getString("name"))) hasHash = true;
        }
        if (!hasHash) try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE satellite_chunks_v1 ADD COLUMN content_hash TEXT");
        }
    }

    /** Called after a complete PNG has arrived from a client world render. */
    public synchronized boolean putImage(int x, int z, int res, byte[] png) throws IOException, SQLException {
        validateResolution(res);
        if (png == null || png.length == 0 || png.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Invalid image size");
        if (png.length < 24 || png[0] != (byte) 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            throw new IllegalArgumentException("Only PNG images are supported");
        }
        var image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null || image.getWidth() != res || image.getHeight() != res) {
            throw new IllegalArgumentException("Image dimensions do not match resolution");
        }
        StoredImage current = storedImage(x, z);
        if (current != null && current.res() > res) return false;
        String hash = sha256(png);
        Path target = imagePath(x, z, res);
        if (current != null && current.res() == res && hash.equals(current.hash()) && Files.isRegularFile(target)) return false;
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "capture-", ".tmp");
        try {
            Files.write(temporary, png);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        long revision;
        try (var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COALESCE(MAX(revision), 0) + 1 FROM satellite_chunks_v1")) {
            revision = result.next() ? result.getLong(1) : 1;
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO satellite_chunks_v1 (chunk_x, chunk_z, resolution, updated_at_ms, revision, content_hash)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(chunk_x, chunk_z) DO UPDATE SET
                  resolution=excluded.resolution, updated_at_ms=excluded.updated_at_ms,
                  revision=excluded.revision, content_hash=excluded.content_hash
                """)) {
            statement.setInt(1, x);
            statement.setInt(2, z);
            statement.setInt(3, res);
            statement.setLong(4, System.currentTimeMillis());
            statement.setLong(5, revision);
            statement.setString(6, hash);
            statement.executeUpdate();
        }
        return true;
    }

    public Changes changes(Long lastChange, Integer limit, Integer offset) throws SQLException {
        int pageSize = limit == null ? PAGE_SIZE : limit;
        if (pageSize < 1 || pageSize > PAGE_SIZE) throw new IllegalArgumentException("Invalid limit");
        int pageOffset = offset == null ? 0 : offset;
        if (pageOffset < 0) throw new IllegalArgumentException("Invalid offset");
        long cursor = lastChange == null ? 0 : lastChange;
        List<Change> chunks = new ArrayList<>();
        try (var statement = connection.prepareStatement("""
                SELECT chunk_x, chunk_z, resolution, updated_at_ms, revision
                FROM satellite_chunks_v1 WHERE revision > ? ORDER BY revision, chunk_x, chunk_z
                LIMIT ? OFFSET ?
                """)) {
            statement.setLong(1, cursor);
            statement.setInt(2, pageSize + 1);
            statement.setInt(3, pageOffset);
            try (var result = statement.executeQuery()) {
                while (result.next()) chunks.add(new Change(result.getInt(1), result.getInt(2),
                        result.getInt(3), result.getLong(4), result.getLong(5)));
            }
        }
        boolean partial = chunks.size() > pageSize;
        List<Change> page = partial ? chunks.subList(0, pageSize) : chunks;
        long next = page.isEmpty() ? cursor : page.get(page.size() - 1).revision();
        return new Changes(1, lastChange == null, next, partial,
                partial ? pageOffset + pageSize : null, page);
    }

    public byte[] image(int x, int z, int res) throws IOException, SQLException {
        validateResolution(res);
        StoredImage stored = storedImage(x, z);
        if (stored == null || stored.res() != res) return null;
        Path image = imagePath(x, z, res);
        return Files.isRegularFile(image) && Files.size(image) <= MAX_IMAGE_BYTES ? Files.readAllBytes(image) : null;
    }

    public boolean needsCapture(int x, int z, int res) throws SQLException {
        validateResolution(res);
        StoredImage stored = storedImage(x, z);
        return stored == null || stored.res() < res || !Files.isRegularFile(imagePath(x, z, stored.res()));
    }

    private StoredImage storedImage(int x, int z) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT resolution, content_hash FROM satellite_chunks_v1 WHERE chunk_x = ? AND chunk_z = ?")) {
            statement.setInt(1, x);
            statement.setInt(2, z);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new StoredImage(result.getInt(1), result.getString(2)) : null;
            }
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private Path imagePath(int x, int z, int res) {
        return root.resolve(Integer.toString(x)).resolve(Integer.toString(z)).resolve(res + ".png");
    }

    public static void validateResolution(int res) {
        if (res < 64 || res > 1024 || (res & (res - 1)) != 0) throw new IllegalArgumentException("Invalid resolution");
    }
}
