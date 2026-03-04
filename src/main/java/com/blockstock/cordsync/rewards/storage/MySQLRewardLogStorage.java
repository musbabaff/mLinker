package com.blockstock.cordsync.rewards.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.blockstock.cordsync.CordSync;

public class MySQLRewardLogStorage implements RewardLogStorage {

    private final CordSync plugin;
    private Connection connection;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MySQLRewardLogStorage(CordSync plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private void connect() {
        try {
            String host = plugin.getConfig().getString("storage.mysql.host");
            int port = plugin.getConfig().getInt("storage.mysql.port");
            String database = plugin.getConfig().getString("storage.mysql.database");
            String username = plugin.getConfig().getString("storage.mysql.username");
            String password = plugin.getConfig().getString("storage.mysql.password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("✅ MySQL reward log connection established (" + database + ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Failed to establish MySQL reward log connection: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS reward_logs (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        timestamp DATETIME NOT NULL,
                        reward_type VARCHAR(64) NOT NULL,
                        details TEXT
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Failed to create MySQL reward_logs table: " + e.getMessage());
        }
    }

    @Override
    public void log(UUID player, String rewardType, String details) {
        String sql = "INSERT INTO reward_logs (player_uuid, timestamp, reward_type, details) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setString(2, dateFormat.format(new Date()));
            ps.setString(3, rewardType);
            ps.setString(4, details);
            ps.executeUpdate();

            plugin.getLogger().info("🧾 [" + rewardType + "] reward logged to MySQL: " + player);
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Failed to log reward to MySQL: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("💾 MySQL reward log connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Failed to close MySQL connection: " + e.getMessage());
        }
    }
}
