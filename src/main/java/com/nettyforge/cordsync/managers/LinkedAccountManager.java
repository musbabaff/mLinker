package com.nettyforge.cordsync.managers;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.utils.MessageUtil;

public class LinkedAccountManager {

    private final CordSync plugin;
    private final File file;
    private final FileConfiguration data;

    public LinkedAccountManager(CordSync plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "linked-accounts.yml");

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(MessageUtil.format("storage.file-create-error", java.util.Map.of("file", "linked-accounts.yml", "error", e.getMessage())));
            }
        }

        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void setLinkedAccount(UUID uuid, String playerName, String discordId) {
        data.set("linked-accounts." + uuid + ".discord-id", discordId);
        data.set("linked-accounts." + uuid + ".username", playerName);
        save();
    }

    public boolean isPlayerLinked(UUID uuid) {
        return data.contains("linked-accounts." + uuid + ".discord-id");
    }

    public boolean isDiscordLinked(String discordId) {
        if (!data.contains("linked-accounts")) return false;

        Set<String> keys = data.getConfigurationSection("linked-accounts").getKeys(false);
        for (String key : keys) {
            String stored = data.getString("linked-accounts." + key + ".discord-id");
            if (stored != null && stored.equals(discordId)) {
                return true;
            }
        }
        return false;
    }

    public void removeLinkedAccount(UUID uuid) {
        data.set("linked-accounts." + uuid, null);

        if (data.getConfigurationSection("linked-accounts") != null &&
            data.getConfigurationSection("linked-accounts").getKeys(false).isEmpty()) {
            data.set("linked-accounts", null);
        }

        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(MessageUtil.format("storage.file-save-error", java.util.Map.of("file", "linked-accounts.yml", "error", e.getMessage())));
        }
    }
}


