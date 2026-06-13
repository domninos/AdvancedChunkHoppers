package net.omni.ach.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.omni.ach.AdvancedChunkHoppers;
import net.omni.ach.util.ItemSerializationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final AdvancedChunkHoppers plugin;

    private HikariDataSource dataSource;

    public DatabaseManager(AdvancedChunkHoppers plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        File dbFile = new File(plugin.getDataFolder(), "hoppers.db");

        if (!dbFile.exists()) {
            try {
                if (dbFile.createNewFile())
                    plugin.getLogger().log(Level.INFO, "Successfully created hoppers.db!");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create hoppers.db!", e);
            }
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("ChunkHopperPool");
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000); // 5 seconds timeout
        config.setLeakDetectionThreshold(2000); // checks if a connection is bleeding

        this.dataSource = new HikariDataSource(config);

        // create table
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS chunk_hoppers (
                        location_key TEXT PRIMARY KEY,
                        owner_uuid TEXT,
                        base64_items TEXT);
                    """);
            stmt.execute("PRAGMA journal_mode=WAL");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while creating the database!", e);
        }

        plugin.sendConsole("<green>Successfully initialized database.</green>");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new SQLException("DataSource not initialized");

        return dataSource.getConnection();
    }

    public CompletableFuture<List<ItemStack>> fetchItems(Location location) {
        String locationKey = getLocationKey(location);

        if (locationKey.isBlank())
            return null;

        CompletableFuture<List<ItemStack>> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String query = "SELECT base64_items FROM chunk_hoppers WHERE location_key = ?";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, locationKey);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String base64 = rs.getString("base64_items");
                        if (base64 != null && !base64.isEmpty())
                            future.complete(ItemSerializationUtil.fromBase64(base64));
                        else
                            future.complete(List.of());
                    } else {
                        future.complete(List.of());
                    }
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
                plugin.getLogger().log(Level.SEVERE, "Error while fetching items from the database!", e);
            }
        });

        return future;
    }

    private String getLocationKey(Location location) {
        if (location == null)
            return "";

        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public void saveAsync(Location location, List<ItemStack> items) {
        String locationKey = getLocationKey(location);

        if (locationKey.isBlank())
            return;

        String base64Items = ItemSerializationUtil.toBase64(items);

        if (base64Items.isBlank())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String query = "INSERT OR REPLACE INTO chunk_hoppers (location_key, base64_items) VALUES (?, ?);";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, locationKey);
                stmt.setString(2, base64Items);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error while saving the database!", e);
            }
        });
    }

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
    }
}
