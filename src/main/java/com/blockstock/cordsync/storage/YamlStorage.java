package com.blockstock.cordsync.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blockstock.cordsync.CordSync;

public class YamlStorage implements StorageProvider, Migratable {

    private final CordSync plugin;
    private final File file;
    private final FileConfiguration data;

    public YamlStorage(CordSync plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "linked-accounts.yml");

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                plugin.getLogger().info("✅ linked-accounts.yml created.");
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Failed to create linked-accounts.yml: " + e.getMessage());
            }
        }

        this.data = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void setLinkedAccount(UUID uuid, String playerName, String discordId) {
        data.set("linked-accounts." + uuid + ".username", playerName);
        data.set("linked-accounts." + uuid + ".discord-id", discordId);
        save();
    }

    @Override
    public String getDiscordId(UUID uuid) {
        return data.getString("linked-accounts." + uuid + ".discord-id");
    }

    @Override
    public UUID getPlayerUUID(String discordId) {
        if (!data.contains("linked-accounts"))
            return null;

        Set<String> keys = data.getConfigurationSection("linked-accounts").getKeys(false);
        for (String key : keys) {
            String storedId = data.getString("linked-accounts." + key + ".discord-id");
            if (storedId != null && storedId.equalsIgnoreCase(discordId)) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public void removeLinkedAccount(UUID uuid) {
        data.set("linked-accounts." + uuid, null);

        if (data.getConfigurationSection("linked-accounts") != null &&
                data.getConfigurationSection("linked-accounts").getKeys(false).isEmpty()) {
            data.set("linked-accounts", null);
        }

        save();
    }

    @Override
    public boolean isPlayerLinked(UUID uuid) {
        return data.contains("linked-accounts." + uuid + ".discord-id");
    }

    @Override
    public boolean isDiscordLinked(String discordId) {
        if (!data.contains("linked-accounts"))
            return false;

        Set<String> keys = data.getConfigurationSection("linked-accounts").getKeys(false);
        for (String key : keys) {
            String storedId = data.getString("linked-accounts." + key + ".discord-id");
            if (storedId != null && storedId.equalsIgnoreCase(discordId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<UUID, Migratable.LinkedData> loadAllLinkedAccounts() {
        Map<UUID, Migratable.LinkedData> map = new HashMap<>();
        if (!data.contains("linked-accounts"))
            return map;

        Set<String> keys = data.getConfigurationSection("linked-accounts").getKeys(false);
        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);
                String username = data.getString("linked-accounts." + key + ".username");
                String discordId = data.getString("linked-accounts." + key + ".discord-id");
                if (discordId != null && username != null) {
                    map.put(uuid, new Migratable.LinkedData(username, discordId));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return map;
    }

    @Override
    public void importLinkedAccounts(Map<UUID, Migratable.LinkedData> accounts) {
        for (Map.Entry<UUID, Migratable.LinkedData> e : accounts.entrySet()) {
            UUID uuid = e.getKey();
            Migratable.LinkedData ld = e.getValue();
            data.set("linked-accounts." + uuid + ".username", ld.playerName);
            data.set("linked-accounts." + uuid + ".discord-id", ld.discordId);
        }
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Failed to save linked-accounts.yml: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        save();
    }

    // Suistimal korumasÄ± - YAML implementasyonu
    @Override
    public long getUnlinkTimestamp(UUID uuid) {
        return data.getLong("link-history." + uuid + ".unlink-time", 0);
    }

    @Override
    public void setUnlinkTimestamp(UUID uuid, long timestamp) {
        data.set("link-history." + uuid + ".unlink-time", timestamp);
        save();
    }

    @Override
    public int getRelinkCount(UUID uuid) {
        return data.getInt("link-history." + uuid + ".relink-count", 0);
    }

    @Override
    public void incrementRelinkCount(UUID uuid) {
        int current = getRelinkCount(uuid);
        data.set("link-history." + uuid + ".relink-count", current + 1);
        save();
    }

    @Override
    public boolean hasReceivedFirstReward(UUID uuid) {
        return data.getBoolean("link-history." + uuid + ".first-reward-received", false);
    }

    @Override
    public void setFirstRewardReceived(UUID uuid) {
        data.set("link-history." + uuid + ".first-reward-received", true);
        save();
    }
}
