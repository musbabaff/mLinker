package com.blockstock.cordsync.storage;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import com.blockstock.cordsync.CordSync;

public interface StorageProvider {

    void setLinkedAccount(UUID uuid, String playerName, String discordId);

    String getDiscordId(UUID uuid);

    UUID getPlayerUUID(String discordId);

    void removeLinkedAccount(UUID uuid);

    boolean isPlayerLinked(UUID uuid);

    boolean isDiscordLinked(String discordId);

    // Abuse protection - unlink cooldown and relink limit
    long getUnlinkTimestamp(UUID uuid);

    void setUnlinkTimestamp(UUID uuid, long timestamp);

    int getRelinkCount(UUID uuid);

    void incrementRelinkCount(UUID uuid);

    boolean hasReceivedFirstReward(UUID uuid);

    void setFirstRewardReceived(UUID uuid);

    void close();

    default Set<UUID> getAllLinkedPlayers() {
        Set<UUID> result = new HashSet<>();
        try {
            File yamlFile = new File(CordSync.getInstance().getDataFolder(), "linked-accounts.yml");
            if (yamlFile.exists()) {
                YamlConfiguration data = YamlConfiguration.loadConfiguration(yamlFile);
                if (data.contains("linked-accounts")) {
                    for (String key : data.getConfigurationSection("linked-accounts").getKeys(false)) {
                        try {
                            result.add(UUID.fromString(key));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            CordSync.getInstance().getLogger().severe("❌ Error in getAllLinkedPlayers: " + e.getMessage());
        }
        return result;
    }

    default String getPlayerName(UUID uuid) {
        try {
            File yamlFile = new File(CordSync.getInstance().getDataFolder(), "linked-accounts.yml");
            if (yamlFile.exists()) {
                YamlConfiguration data = YamlConfiguration.loadConfiguration(yamlFile);
                return data.getString("linked-accounts." + uuid + ".username", "Unknown");
            }
        } catch (Exception e) {
            CordSync.getInstance().getLogger().severe("❌ Error in getPlayerName: " + e.getMessage());
        }
        return "Unknown";
    }
}