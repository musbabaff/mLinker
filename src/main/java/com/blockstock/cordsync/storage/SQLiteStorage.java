package com.blockstock.cordsync.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.blockstock.cordsync.CordSync;

public class SQLiteStorage implements StorageProvider, Migratable {

    private final CordSync plugin;
    private Connection connection;

    public SQLiteStorage(CordSync plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "CordSync.db");
            if (!dbFile.getParentFile().exists())
                dbFile.getParentFile().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("✅ SQLite connection established: " + dbFile.getName());
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Failed to establish SQLite connection: " + e.getMessage());
        }
    }

    private void createTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS linked_accounts (" +
                    " uuid TEXT PRIMARY KEY," +
                    " username TEXT NOT NULL," +
                    " discord_id TEXT NOT NULL UNIQUE" +
                    ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS link_history (" +
                    " uuid TEXT PRIMARY KEY," +
                    " unlink_time BIGINT DEFAULT 0," +
                    " relink_count INTEGER DEFAULT 0," +
                    " first_reward_received INTEGER DEFAULT 0" +
                    ");");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }

    @Override
    public void setLinkedAccount(UUID uuid, String playerName, String discordId) {
        String sql = "INSERT OR REPLACE INTO linked_accounts (uuid, username, discord_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, discordId);
            ps.executeUpdate();
            plugin.debug("SQLite: Successfully inserted/updated linked profile for UUID " + uuid.toString());
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }

    @Override
    public String getDiscordId(UUID uuid) {
        String sql = "SELECT discord_id FROM linked_accounts WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("discord_id");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public UUID getPlayerUUID(String discordId) {
        String sql = "SELECT uuid FROM linked_accounts WHERE discord_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return UUID.fromString(rs.getString("uuid"));
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void removeLinkedAccount(UUID uuid) {
        String sql = "DELETE FROM linked_accounts WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }

    @Override
    public boolean isPlayerLinked(UUID uuid) {
        String sql = "SELECT 1 FROM linked_accounts WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean isDiscordLinked(String discordId) {
        String sql = "SELECT 1 FROM linked_accounts WHERE discord_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Map<UUID, Migratable.LinkedData> loadAllLinkedAccounts() {
        Map<UUID, Migratable.LinkedData> map = new HashMap<>();
        String sql = "SELECT uuid, username, discord_id FROM linked_accounts";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String username = rs.getString("username");
                String discordId = rs.getString("discord_id");
                map.put(uuid, new Migratable.LinkedData(username, discordId));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void importLinkedAccounts(Map<UUID, Migratable.LinkedData> accounts) {
        String sql = "INSERT OR REPLACE INTO linked_accounts (uuid, username, discord_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (Map.Entry<UUID, Migratable.LinkedData> e : accounts.entrySet()) {
                ps.setString(1, e.getKey().toString());
                ps.setString(2, e.getValue().playerName);
                ps.setString(3, e.getValue().discordId);
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (Exception ignored) {
            }
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }

    public Set<UUID> getAllLinkedPlayers() {
        Set<UUID> uuids = new HashSet<>();
        String sql = "SELECT uuid FROM linked_accounts";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                uuids.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return uuids;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("💾 SQLite connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Failed to close SQLite connection: " + e.getMessage());
        }
    }

    // ===================================================================
    // SUÄ°STÄ°MAL KORUMASI - Unlink cooldown, relink limiti, ilk Ã¶dÃ¼l
    // ===================================================================

    @Override
    public long getUnlinkTimestamp(UUID uuid) {
        String sql = "SELECT unlink_time FROM link_history WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getLong("unlink_time");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void setUnlinkTimestamp(UUID uuid, long timestamp) {
        String sql = "INSERT INTO link_history (uuid, unlink_time) VALUES (?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET unlink_time = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, timestamp);
            ps.setLong(3, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }

    @Override
    public int getRelinkCount(UUID uuid) {
        String sql = "SELECT relink_count FROM link_history WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("relink_count");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void incrementRelinkCount(UUID uuid) {
        String sql = "INSERT INTO link_history (uuid, relink_count) VALUES (?, 1) " +
                "ON CONFLICT(uuid) DO UPDATE SET relink_count = relink_count + 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }

    @Override
    public boolean hasReceivedFirstReward(UUID uuid) {
        String sql = "SELECT first_reward_received FROM link_history WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("first_reward_received") == 1;
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void setFirstRewardReceived(UUID uuid) {
        String sql = "INSERT INTO link_history (uuid, first_reward_received) VALUES (?, 1) " +
                "ON CONFLICT(uuid) DO UPDATE SET first_reward_received = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ SQLite error: " + e.getMessage());
        }
    }
}
